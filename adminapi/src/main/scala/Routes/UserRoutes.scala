package routes

import models.User
import dao.UserDAO

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import spray.json._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

trait UserJsonFormats extends DefaultJsonProtocol {
  implicit val userFormat: RootJsonFormat[User] = jsonFormat3(User.apply)
  implicit val userListFormat: RootJsonFormat[List[User]] = listFormat(userFormat)
}

object UserRoutes extends UserJsonFormats {
  val route: Route =
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
    }
}
