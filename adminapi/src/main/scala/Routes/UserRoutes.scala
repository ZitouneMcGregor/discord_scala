package routes

import models.User
import dao.UserDAO
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import spray.json.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import Repo.UserServerDAO
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._
import org.apache.pekko.http.cors.scaladsl.settings.CorsSettings
import org.apache.pekko.http.cors.scaladsl.model.HttpOriginMatcher
import org.apache.pekko.http.scaladsl.model.HttpMethods._
import org.apache.pekko.http.cors.scaladsl.model.HttpHeaderRange
import scala.collection.immutable.Seq

import spray.json._
import spray.json.DefaultJsonProtocol._

import org.apache.pekko.actor.*
import org.apache.pekko.stream.scaladsl.*
import org.apache.pekko.http.scaladsl.*
import org.apache.pekko.http.cors.scaladsl.CorsDirectives.*
import scala.util.{Success, Failure}

case class UserWithAdmin(user: User, admin: Boolean)

trait UserJsonFormats extends DefaultJsonProtocol {
  implicit val userFormat: RootJsonFormat[User] = jsonFormat4(User.apply)
  implicit val userListFormat: RootJsonFormat[UserWithAdmin] = jsonFormat2(UserWithAdmin.apply)
}

object UserRoutes extends UserJsonFormats {

  val corsSettings: CorsSettings = CorsSettings.defaultSettings
    .withAllowedOrigins(HttpOriginMatcher.*)
    .withAllowedMethods(Seq(GET, POST, PUT, DELETE, OPTIONS))
    .withAllowedHeaders(HttpHeaderRange.*)

  val route: Route = cors(corsSettings) {
    pathPrefix("users") {
      pathEnd {
        post {
          entity(as[User]) { user =>
            onComplete(UserDAO.insertUser(user)) {
              case Success(Right(true)) => complete(StatusCodes.Created -> "User inserted successfully")
              case Success(Right(false)) => complete(StatusCodes.InternalServerError -> "Error inserting user")
              case Success(Left(error)) => complete(StatusCodes.Conflict -> error)
              case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")

            }
          }
        }
      } ~
      path(Segment) { username =>
        get {

          onComplete(UserDAO.getUserByUsername(username))  {
            case Success(Some(user)) => complete(StatusCodes.OK, user)
            case Success(None) => complete(StatusCodes.NotFound, s"User with username $username not found")
            case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
          }
        } ~
        delete {
          onComplete(UserDAO.getUserByUsername(username)) {
            case Success(Some(user)) =>
              val newUsername = s"deletedAccount#${user.id}"
              onComplete(UserDAO.updateUser(username, newUsername, user.password)) {
                case Success(true) =>
                  onComplete(UserDAO.deleteUser(newUsername)) {
                    case Success(true) => complete(StatusCodes.OK -> s"User $username (renamed to $newUsername) deleted successfully")
                    case Success(false) => complete(StatusCodes.NotFound -> s"Failed to delete user $newUsername")
                    case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred while deleting: ${ex.getMessage}")
                  }
                case Success(false) => complete(StatusCodes.NotFound -> s"Failed to update user $username before deletion")
                case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred while renaming: ${ex.getMessage}")
              }
            case Success(None) => complete(StatusCodes.NotFound -> s"User with username $username not found")
            case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
          }
        } ~
        put {
          entity(as[User]) { user =>
            onComplete(UserDAO.updateUser(username, user.username, user.password)) {
              case Success(true) => complete(StatusCodes.OK -> s"User $username updated successfully")
          
              case Success(false) =>complete(StatusCodes.NotFound -> s"User $username not found")

              case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")

            }
          }
        }
      }
    } ~
    pathPrefix("server" / IntNumber / "users") { id_server =>
      get {
        onComplete(UserServerDAO.getAllUserFromServer(id_server)) {
          case Success(userTuples) =>
            val usersWithAdmin = userTuples.map { case (user, admin) =>
              UserWithAdmin(user, admin)
            }
            complete(usersWithAdmin.toJson)
          case Failure(ex) =>
            complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
        }
      } ~
    pathPrefix("server" / IntNumber / "invite") { id_server =>
      get {
      complete(UserServerDAO.getAllUserNotFromServer(id_server))

    }

  }
    }
  }
}