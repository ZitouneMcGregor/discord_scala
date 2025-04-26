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
import cats.syntax.all._

object UserRegistry {


  //#user-case-classes
  final case class User(id: Option[Int], username: String, password: String, deleted: Option[Boolean])
  final case class Users(users: immutable.Seq[User])
  
  // actor protocol
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUser(username: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class GetUserById(id: Int, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(username: String, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class UpdateUser(username: String, newUsername: String, newPassword: String, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class SearchUsers(prefix: String, replyTo: ActorRef[Users]) extends Command

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
    val list: List[User] =
      sql"SELECT * FROM  users ".
      query[User].
      to[List].
      transact(xa).
      attempt
      .map { 
        case Right(mdp) =>
          mdp.map(u => u.copy(password = ""))
        case Left(e) =>
          println(s"[DB ERROR] getUsers: ${e.getMessage}")
          Nil
      }.
      unsafeRunSync()

    Users(list)
  }

  def dbGetUser(xa: Transactor, username: String): Option[User] = {
    sql"SELECT * FROM users WHERE username = ${username}".
      query[User].
      option.
      transact(xa).
      attempt
      .map {
        case Right(Some(u)) => Some(u.copy(password = ""))
        case Right(None) => None
        case Left(e) =>
          println(s"[DB ERROR] getUser($username): ${e.getMessage}")
          None
      }.
      unsafeRunSync()
  }

  def dbGetUserById(xa: Transactor, id: Int): Option[User] = {
    sql"SELECT * FROM users WHERE id = $id".
      query[User].
      option.
      transact(xa).
      attempt
      .map {
        case Right(Some(u)) => Some(u.copy(password = ""))
        case Right(None) => None
        case Left(e) =>
          println(s"[DB ERROR] getUserById($id): ${e.getMessage}")
          None
      }
      .unsafeRunSync()
  }

  
  def dbCreateUser(xa: Transactor, user: User): IO[Either[String, User]] = {
    sql"INSERT INTO users (username, password) VALUES (${user.username}, ${user.password})"
      .update
      .withUniqueGeneratedKeys[User]("id", "username", "password", "deleted")
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => s"Error: Username '${user.username}' already exists."
      }
      .attempt
      .map {
        case Right(either) => either
        case Left(e) =>
          println(s"[DB ERROR] createUser(${user.username}): ${e.getMessage}")
          Left(s"Error: Could not create user '${user.username}'.")
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
      attempt
      .map {
        case Right(count) => count > 0
        case Left(e) =>
          println(s"[DB ERROR] deleteUser($username): ${e.getMessage}")
          false
      }
      .unsafeRunSync()
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
      .attempt
      .map {
        case Right(either) => either
        case Left(e) =>
          println(s"[DB ERROR] updateUser($username): ${e.getMessage}")
          Left(s"Error: Could not update user '$username'.")
      }
      .transact(xa)
  }

  private def dbSearchUsers(xa: Transactor, prefix: String): Users = {
    val pattern = s"${prefix.toLowerCase}%"
    val list: List[User] =
      sql"""
        SELECT id, username, '' AS password, deleted
        FROM users
        WHERE LOWER(username) LIKE $pattern
          AND COALESCE(deleted, false) = false
        ORDER BY username
        LIMIT 10
      """
        .query[User]
        .to[List]
        .transact(xa)
        .attempt
        .map {
          case Right(us) => us.map(u => u.copy(password = ""))
          case Left(e) =>
            println(s"[DB ERROR] searchUsers($prefix): ${e.getMessage}")
            Nil
        }
        .unsafeRunSync()

    Users(list)
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
      case GetUserById(id, replyTo) =>
        replyTo !  GetUserResponse(dbGetUserById(xa, id))
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
      case SearchUsers(prefix, replyTo) =>
        replyTo ! dbSearchUsers(xa, prefix)
        Behaviors.same
    }
}
