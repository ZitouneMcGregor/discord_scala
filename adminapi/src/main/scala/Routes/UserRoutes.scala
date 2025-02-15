package routes

import models.User
import dao.UserDAO
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import spray.json._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import Repo.UserServerDAO
import org.apache.pekko.actor._
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.http.scaladsl._
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._

trait UserJsonFormats extends DefaultJsonProtocol {
  implicit val userFormat: RootJsonFormat[User] = jsonFormat4(User.apply)
  implicit val userListFormat: RootJsonFormat[List[User]] = listFormat(userFormat)
}

object UserRoutes extends UserJsonFormats {
  val route: Route =
    cors() {
      path("users") {
        post {
          entity(as[User]) { user =>
            if (UserDAO.insertUser(user)) {
              complete(StatusCodes.Created -> "User inserted successfully")
            } else {
              complete(StatusCodes.InternalServerError -> "Error inserting user")
            }
          }
        } ~
        get {
          complete(UserDAO.getAllUsers)
        }
      } ~
      path("user" / Segment) { username =>
        get {
          UserDAO.getUserByUsername(username) match {
            case Some(user) => complete(StatusCodes.OK, user)
            case None => complete(StatusCodes.NotFound, s"User with username $username not found")
          }
        } ~
        delete {
          if (UserDAO.deleteUser(username)) {
            complete(StatusCodes.OK -> s"User $username deleted successfully")
          } else {
            complete(StatusCodes.NotFound -> s"User $username not found or already deleted")
          }
        } ~
        put {
          entity(as[User]) { user =>
            if (UserDAO.updateUser(username, user.username, user.password)) {
              complete(StatusCodes.OK -> s"User $username updated successfully")
            } else {
              complete(StatusCodes.NotFound -> s"User $username not found")
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
