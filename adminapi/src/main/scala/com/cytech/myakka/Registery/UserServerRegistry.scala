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
import com.cytech.myakka.routes.{User, Users} // Import new case classes
import doobie.postgres.sqlstate

final case class UserServer(id: Option[Int], user_id: Int, server_id: Int, admin: Boolean)
final case class UserServers(users: immutable.Seq[UserServer])
final case class ActionPerformed(success: Boolean, description: String)

object UserServerRegistry {
  sealed trait Command
  final case class GetUsers(serverId: Int, replyTo: ActorRef[UserServers]) extends Command
  final case class AddUser(userServer: UserServer, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class RemoveUser(serverId: Int, userId: Int, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class UpdateAdmin(serverId: Int, userId: Int, admin: Boolean, replyTo: ActorRef[ActionPerformed]) extends Command
  // New command
  final case class GetUsersNotInServer(serverId: Int, replyTo: ActorRef[Users]) extends Command

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

  def dbGetUsers(xa: Transactor, serverId: Int): UserServers = {
    UserServers(
      sql"SELECT id, user_id, server_id, admin FROM server_user WHERE server_id = $serverId"
        .query[UserServer]
        .to[List]
        .transact(xa)
        .unsafeRunSync()
    )
  }

  def dbAddUser(xa: Transactor, userServer: UserServer): IO[Either[String, UserServer]] = {
    sql"INSERT INTO server_user (user_id, server_id, admin) VALUES (${userServer.user_id}, ${userServer.server_id}, ${userServer.admin})"
      .update
      .withUniqueGeneratedKeys[UserServer]("id", "user_id", "server_id", "admin")
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION =>
          s"User ${userServer.user_id} already exists in server ${userServer.server_id}"
        case sqlstate.class23.FOREIGN_KEY_VIOLATION =>
          s"Invalid user_id ${userServer.user_id} or server_id ${userServer.server_id}: referenced record does not exist"
      }
      .transact(xa)
  }

  def dbRemoveUser(xa: Transactor, serverId: Int, userId: Int): Int = {
    sql"DELETE FROM server_user WHERE server_id = $serverId AND user_id = $userId"
      .update
      .run
      .transact(xa)
      .unsafeRunSync()
  }

  def dbUpdateAdmin(xa: Transactor, serverId: Int, userId: Int, admin: Boolean): Int = {
    sql"UPDATE server_user SET admin = $admin WHERE server_id = $serverId AND user_id = $userId"
      .update
      .run
      .transact(xa)
      .unsafeRunSync()
  }

  // New function to get users not in a server
  def dbGetUsersNotInServer(xa: Transactor, serverId: Int): Users = {
    Users(
      sql"""
        SELECT id, username 
        FROM users 
        WHERE id NOT IN (SELECT user_id FROM server_user WHERE server_id = $serverId)
        AND deleted = false
      """
        .query[User]
        .to[List]
        .transact(xa)
        .unsafeRunSync()
    )
  }

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetUsers(serverId, replyTo) =>
        replyTo ! dbGetUsers(xa, serverId)
        Behaviors.same

      case AddUser(userServer, replyTo) =>
        dbAddUser(xa, userServer).unsafeRunSync() match {
          case Right(created) =>
            replyTo ! ActionPerformed(true, s"User ${userServer.user_id} added to server ${userServer.server_id}")
          case Left(error) =>
            replyTo ! ActionPerformed(false, error)
        }
        Behaviors.same

      case RemoveUser(serverId, userId, replyTo) =>
        val rowsAffected = dbRemoveUser(xa, serverId, userId)
        if (rowsAffected > 0) {
          replyTo ! ActionPerformed(true, s"User $userId removed from server $serverId")
        } else {
          replyTo ! ActionPerformed(false, s"User $userId not found in server $serverId")
        }
        Behaviors.same

      case UpdateAdmin(serverId, userId, admin, replyTo) =>
        val rowsAffected = dbUpdateAdmin(xa, serverId, userId, admin)
        if (rowsAffected > 0) {
          replyTo ! ActionPerformed(true, s"User $userId admin status updated to $admin in server $serverId")
        } else {
          replyTo ! ActionPerformed(false, s"User $userId not found in server $serverId")
        }
        Behaviors.same

      // New case for users not in server
      case GetUsersNotInServer(serverId, replyTo) =>
        replyTo ! dbGetUsersNotInServer(xa, serverId)
        Behaviors.same
    }
}