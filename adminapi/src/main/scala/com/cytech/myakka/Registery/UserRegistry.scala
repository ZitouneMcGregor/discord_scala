package com.cytech.myakka.registery

//#user-registry-actor
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





object UserRegistry {


  //#user-case-classes
  final case class User(id: Option[Int], username: String, password: String, deleted: Option[Boolean])
  final case class Users(users: immutable.Seq[User])


  // actor protocol
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUser(username: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(username: String, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class UpdateUser(username: String, newUsername: String, newPassword: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetUserResponse(maybeUser: Option[User])
  final case class ActionPerformed(success : Boolean,description: String)

  def apply(pgConfig: PostgresConfig): Behavior[Command] = {
    registry(transactor(pgConfig))
  }

  type Transactor = doobie.Transactor.Aux[IO, Unit]
  def transactor(pgConfig: PostgresConfig): Transactor = {
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url =  pgConfig.url,
      user = pgConfig.user, 
      password =pgConfig.pass,
      logHandler = None
    )
  }

  def dbGetUsers(xa: Transactor): Users = {
    Users(
      sql"SELECT * FROM  users ".
      query[User].
      to[List].
      transact(xa).
      unsafeRunSync()
      .map { user => user.copy(password = "") }
    )
  }

  def dbGetUser(xa: Transactor, username: String): Option[User] = {
    sql"SELECT * FROM users WHERE username = ${username}".
      query[User].
      option.
      transact(xa).
      unsafeRunSync()
  }



  
  def dbCreateUser(xa: Transactor, user: User): IO[Either[String, User]] = {
    sql"INSERT INTO users (username, password) VALUES (${user.username}, ${user.password})"
      .update
      .withUniqueGeneratedKeys[User]("id", "username", "password", "deleted")
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => s"Error: Username '${user.username}' already exists."
      }
      .transact(xa)
  }
  

  def dbDeleteUser(xa: Transactor, username: String): Unit = {
    val newUsername : String = username + "@DELETED_USER"
    sql"""UPDATE users 
    SET deleted = true, username = $newUsername
    WHERE username = $username
    """.
      update.
      run.
      transact(xa).
      unsafeRunSync()
  }

  def dbUpdateUser(xa: Transactor, username: String, newUsername: String, newPassword: String): IO[Either[String, Int]] = {
    sql"""
      UPDATE users
      SET username = $newUsername, password = $newPassword
      WHERE username = $username
    """.update.run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => s" Error: Username '$newUsername' is already taken."
      }
      .transact(xa)
  }
  

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        replyTo ! dbGetUsers(xa)
        Behaviors.same
      case CreateUser(user, replyTo) =>
        dbCreateUser(xa, user).unsafeRunSync() match {
            case Right(createdUser) =>
              replyTo ! ActionPerformed(true,s" User ${createdUser.username} created.")
            case Left(errorMessage) =>
              replyTo ! ActionPerformed(false,errorMessage)
          }
          Behaviors.same
      case GetUser(username, replyTo) =>
        replyTo !  GetUserResponse(dbGetUser(xa, username))
        Behaviors.same
      case DeleteUser(username, replyTo) =>
        replyTo ! ActionPerformed(true,s"User $username deleted.")
        dbDeleteUser(xa, username)
        Behaviors.same
      case UpdateUser(username, newUsername, newPassword, replyTo) =>
        dbUpdateUser(xa, username, newUsername, newPassword).unsafeRunSync() match {
          case Right(rowsAffected) if rowsAffected > 0 =>
             replyTo ! ActionPerformed(true," User $username updated to $newUsername.")
          case Right(_) =>
            replyTo ! ActionPerformed(false,s" Error: User $username not found.")
          case Left(errorMessage) =>
            replyTo ! ActionPerformed(false,errorMessage)
        }
        Behaviors.same
    }
}
//#user-registry-actor
