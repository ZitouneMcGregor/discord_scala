package com.cytech.myakka.registry

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

final case class Server(id: Option[Int], name: String, img: Option[String])
final case class Servers(servers: immutable.Seq[Server])

object ServerRegistry {
  sealed trait Command
  final case class GetServers(replyTo: ActorRef[Servers]) extends Command
  final case class CreateServer(server: Server, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetServer(id: Int, replyTo: ActorRef[GetServerResponse]) extends Command
  final case class DeleteServer(id: Int, replyTo: ActorRef[ActionPerformed]) extends Command
  
  final case class GetServerResponse(maybeServer: Option[Server])
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

  def dbGetServers(xa: Transactor): Servers = {
    Servers(
      sql"SELECT * FROM servers".
      query[Server].
      to[List].
      transact(xa).
      unsafeRunSync()
    )
  }

  def dbGetServer(xa: Transactor, id: Int): Option[Server] = {
    sql"SELECT * FROM servers WHERE id = $id".
      query[Server].
      option.
      transact(xa).
      unsafeRunSync()
  }

  def dbCreateServer(xa: Transactor, server: Server): IO[Either[String, Server]] = {
    sql"INSERT INTO servers (name, img) VALUES (${server.name}, ${server.img})"
      .update
      .withUniqueGeneratedKeys[Server]("id", "name", "img")
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => s"Error: Server name '${server.name}' already exists."
      }
      .transact(xa)
  }

  def dbDeleteServer(xa: Transactor, id: Int): Unit = {
    sql"DELETE FROM servers WHERE id = $id".
      update.
      run.
      transact(xa).
      unsafeRunSync()
  }

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
        case GetServers(replyTo) => 
            replyTo ! dbGetServers(xa)
            Behaviors.same
        case _ => 
            println("Received an unknown command.")
            Behaviors.same
    }
}
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



final case class Server(id: Option[Int], name: String, img: Option[String])
final case class Servers(servers: immutable.Seq[Server])



object ServerRegistry {
  sealed trait Command
  final case class GetServers(replyTo: ActorRef[Servers]) extends Command
  final case class CreateServer(server: Server, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetServer(id: Int, replyTo: ActorRef[GetServerResponse]) extends Command
  final case class DeleteServer(id: Int, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class UpdateServer(id: Int, newName: String, newImg: String, replyTo: ActorRef[ActionPerformed]) extends Command


  final case class GetServerResponse(maybeServer: Option[Server])
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

  def dbGetServers(xa: Transactor): Servers = {
    Servers(
      sql"SELECT * FROM servers".
      query[Server].
      to[List].
      transact(xa).
      unsafeRunSync()
    )
  }

  def dbGetServer(xa: Transactor, id: Int): Option[Server] = {
    sql"SELECT * FROM servers WHERE id = $id".
      query[Server].
      option.
      transact(xa).
      unsafeRunSync()
  }

  def dbCreateServer(xa: Transactor, server: Server): IO[Either[String, Server]] = {
    sql"INSERT INTO servers (name, img) VALUES (${server.name}, ${server.img})"
      .update
      .withUniqueGeneratedKeys[Server]("id", "name", "img")
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => s"Error: Server name '${server.name}' already exists."
      }
      .transact(xa)
  }

  def dbDeleteServer(xa: Transactor, id: Int): Unit = {
    sql"DELETE FROM servers WHERE id = $id".
      update.
      run.
      transact(xa).
      unsafeRunSync()
  }

  def dbUpdateServer(xa: Transactor, id: Int, newName: String, newImg: String): IO[Either[String, Int]] = {
    sql"""
      UPDATE servers
      SET name = $newName, img=$newImg
      WHERE id=$id
      """.update.run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => s" Error: Username '$newName' is already taken"

      }.transact(xa)
  }

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
        case GetServers(replyTo) => 
            replyTo ! dbGetServers(xa)
            Behaviors.same
        case CreateServer(server, replyTo) =>
          dbCreateServer(xa, server).unsafeRunSync() match {
              case Right(createdServer) =>
                replyTo ! ActionPerformed(true,s" Server ${createdServer.name} created.")
              case Left(errorMessage) =>
                replyTo ! ActionPerformed(false,errorMessage)
            }
            Behaviors.same
        case GetServer(id, replyTo) =>
          replyTo ! GetServerResponse(dbGetServer(xa, id))
          Behaviors.same
        case DeleteServer(id, replyTo) =>
          replyTo ! ActionPerformed(true,s"Server $id deleted.")
          dbDeleteServer(xa, id)
          Behaviors.same
        case UpdateServer(id, newName, newImg, replyTo) =>
          dbUpdateServer(xa, id, newName, newImg).unsafeRunSync() match {
          case Right(rowsAffected) if rowsAffected > 0 =>
             replyTo ! ActionPerformed(true," Server $id updated to $newName.")
          case Right(_) =>
            replyTo ! ActionPerformed(false,s" Error: Server $id not found.")
          case Left(errorMessage) =>
            replyTo ! ActionPerformed(false,errorMessage)
            
          }
          Behaviors.same
        case _ => 
            println("Received an unknown command.")
            Behaviors.same
    }
}
