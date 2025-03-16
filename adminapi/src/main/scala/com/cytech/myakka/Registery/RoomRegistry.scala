package com.cytech.myakka.registery

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import scala.collection.immutable
import doobie.*
import doobie.implicits.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cytech.myakka.configuration.PostgresConfig
import doobie.postgres.sqlstate
import scala.util.Try

// Case class for Room (no capacity)
final case class Room(id: Option[Int], name: String, serverId: Int)
final case class Rooms(rooms: immutable.Seq[Room])

object RoomRegistry {
  // Actor protocol
  sealed trait Command
  final case class GetRooms(replyTo: ActorRef[Rooms]) extends Command
  final case class GetRoom(roomId: String, replyTo: ActorRef[GetRoomResponse]) extends Command
  final case class CreateRoom(room: Room, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class DeleteRoom(roomId: String, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class UpdateRoom(roomId: String, room: Room, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetRoomResponse(maybeRoom: Option[Room])
  final case class ActionPerformed(success: Boolean, description: String)

  def apply(pgConfig: PostgresConfig): Behavior[Command] = {
    registry(transactor(pgConfig))
  }

  type Transactor = doobie.Transactor.Aux[IO, Unit]
  def transactor(pgConfig: PostgresConfig): Transactor = {
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = pgConfig.url,
      user = pgConfig.user,
      password = pgConfig.pass,
      logHandler = None
    )
  }

  def dbGetRooms(xa: Transactor): Rooms = {
    Rooms(
      sql"SELECT id, name, id_server FROM room".
        query[Room].
        to[List].
        transact(xa).
        unsafeRunSync()
    )
  }

  def dbGetRoom(xa: Transactor, roomId: String): Option[Room] = {
    Try(roomId.toInt).toOption.flatMap { id =>
      sql"SELECT id, name, id_server FROM room WHERE id = $id".
        query[Room].
        option.
        transact(xa).
        unsafeRunSync()
    }
  }

  def dbCreateRoom(xa: Transactor, room: Room): IO[Either[String, Room]] = {
    sql"INSERT INTO room (name, id_server) VALUES (${room.name}, ${room.serverId})".
      update.
      withUniqueGeneratedKeys[Room]("id", "name", "id_server").
      attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => s"Error: Room '${room.name}' already exists."
      }.
      transact(xa)
  }

  def dbDeleteRoom(xa: Transactor, roomId: String): Option[Int] = {
    Try(roomId.toInt).toOption.map { id =>
      sql"DELETE FROM room WHERE id = $id".
        update.
        run.
        transact(xa).
        unsafeRunSync()
    }
  }

  def dbUpdateRoom(xa: Transactor, roomId: String, room: Room): IO[Either[String, Int]] = {
    Try(roomId.toInt).toOption match {
      case Some(id) =>
        sql"""
          UPDATE room
          SET name = ${room.name}, id_server = ${room.serverId}
          WHERE id = $id
        """.update.run.
          attemptSomeSqlState {
            case sqlstate.class23.UNIQUE_VIOLATION => s"Error: Room name '${room.name}' is already taken."
          }.
          transact(xa)
      case None =>
        IO.pure(Left(s"Error: Invalid room ID '$roomId'"))
    }
  }

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetRooms(replyTo) =>
        replyTo ! dbGetRooms(xa)
        Behaviors.same
      case GetRoom(roomId, replyTo) =>
        replyTo ! GetRoomResponse(dbGetRoom(xa, roomId))
        Behaviors.same
      case CreateRoom(room, replyTo) =>
        dbCreateRoom(xa, room).unsafeRunSync() match {
          case Right(createdRoom) =>
            replyTo ! ActionPerformed(true, s"Room ${createdRoom.name} created.")
          case Left(errorMessage) =>
            replyTo ! ActionPerformed(false, errorMessage)
        }
        Behaviors.same
      case DeleteRoom(roomId, replyTo) =>
        dbDeleteRoom(xa, roomId) match {
          case Some(rowsAffected) if rowsAffected > 0 =>
            replyTo ! ActionPerformed(true, s"Room $roomId deleted.")
          case _ =>
            replyTo ! ActionPerformed(false, s"Error: Room $roomId not found or invalid.")
        }
        Behaviors.same
      case UpdateRoom(roomId, room, replyTo) =>
        dbUpdateRoom(xa, roomId, room).unsafeRunSync() match {
          case Right(rowsAffected) if rowsAffected > 0 =>
            replyTo ! ActionPerformed(true, s"Room $roomId updated.")
          case Right(_) =>
            replyTo ! ActionPerformed(false, s"Error: Room $roomId not found.")
          case Left(errorMessage) =>
            replyTo ! ActionPerformed(false, errorMessage)
        }
        Behaviors.same
    }
}