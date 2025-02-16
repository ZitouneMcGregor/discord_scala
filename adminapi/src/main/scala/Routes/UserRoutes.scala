package routes

import models.User
import dao.UserDAO
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import spray.json.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import Repo.UserServerDAO
import org.apache.pekko.actor.*
import org.apache.pekko.stream.scaladsl.*
import org.apache.pekko.http.scaladsl.*
import org.apache.pekko.http.cors.scaladsl.CorsDirectives.*
import scala.util.{Success, Failure}


trait UserJsonFormats extends DefaultJsonProtocol {
  given userFormat: RootJsonFormat[User] = jsonFormat4(User.apply)
  given userListFormat: RootJsonFormat[List[User]] = listFormat(userFormat)
}

object UserRoutes extends UserJsonFormats {
  val route: Route =
    cors() {
      path("users") {
        post {
          entity(as[User]) { user =>
            onComplete(UserDAO.insertUser(user)) {
              case Success(true) => complete(StatusCodes.Created -> "User inserted successfully")
            
             case Success(false) => complete(StatusCodes.InternalServerError -> "Error inserting user")

            case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")

            }
          }
        } ~
        get {
          complete(UserDAO.getAllUsers)
        }
      } ~
      path("user" / Segment) { username =>
        get {
          onComplete(UserDAO.getUserByUsername(username))  {
            case Success(Some(user)) => complete(StatusCodes.OK, user)
            case Success(None) => complete(StatusCodes.NotFound, s"User with username $username not found")
            case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")

          }
        } ~
        delete {
          onComplete(UserDAO.deleteUser(username)) {
            case Success(true) =>complete(StatusCodes.OK -> s"User $username deleted successfully")
            case Success(false) => complete(StatusCodes.NotFound -> s"User $username not found or already deleted")
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
      } ~
      path("server" / IntNumber / "users") { id_server =>
        get {
          complete(UserServerDAO.getAllUserFromServer(id_server))
        }
      }
    }
}
