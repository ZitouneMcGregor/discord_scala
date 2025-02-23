package routes

import models.Server
import dao.ServerDAO
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import spray.json.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._
import org.apache.pekko.http.cors.scaladsl.settings.CorsSettings
import org.apache.pekko.http.cors.scaladsl.model.HttpOriginMatcher
import scala.collection.immutable.Seq
import org.apache.pekko.http.scaladsl.model.HttpMethods._
import org.apache.pekko.http.cors.scaladsl.model.HttpHeaderRange
import Repo.UserServerDAO
import scala.util.{Success, Failure}
import org.apache.pekko.actor.*
import org.apache.pekko.stream.scaladsl.*
import org.apache.pekko.http.scaladsl.*

trait ServerJsonFormats extends DefaultJsonProtocol {
  given serverFormat: RootJsonFormat[Server] = jsonFormat3(Server.apply)
  given serverListFormat: RootJsonFormat[List[Server]] = listFormat(serverFormat)
}

object ServerRoutes extends ServerJsonFormats {

  val corsSettings: CorsSettings = CorsSettings.defaultSettings
    .withAllowedOrigins(HttpOriginMatcher.*)
    .withAllowedMethods(Seq(GET, POST, PUT, DELETE, OPTIONS))
    .withAllowedHeaders(HttpHeaderRange.*)

  val route: Route =
    cors(corsSettings) {
      pathPrefix("server") {
        pathEnd {
          post {
            entity(as[Server]) { server =>
              onComplete(ServerDAO.insertServer(server)) {
                case Success(true)  => complete(StatusCodes.Created -> "Server inserted successfully")
                case Success(false) => complete(StatusCodes.InternalServerError -> "Error inserting Server in the db")
                case Failure(ex)    => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
              }
            }
          } ~
          get {
            complete(ServerDAO.getAllServer)
          }
        } ~
        path(IntNumber) { id =>
          put {
            entity(as[Server]) { server =>
              val updatedServer = server.copy(id = Some(id))
              onComplete(ServerDAO.updateServer(updatedServer)) {
                case Success(true)  => complete(StatusCodes.OK -> "Server updated successfully")
                case Success(false) => complete(StatusCodes.InternalServerError -> "Error updating Server in the db")
                case Failure(ex)    => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
              }
            }
          } ~
          delete {
            onComplete(ServerDAO.deleteServer(id)) {
              case Success(true)  => complete(StatusCodes.OK -> s"Server with ID $id deleted successfully")
              case Success(false) => complete(StatusCodes.NotFound -> s"Server with ID $id not found")
              case Failure(ex)    => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
            }
          } ~
          get {
            onComplete(ServerDAO.getServerById(id)) {
              case Success(Some(server)) => complete(server)
              case Success(None)         => complete(StatusCodes.NotFound -> s"Server with ID $id not found")
              case Failure(ex)           => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
            }
          }
        } ~
        path("search" / Segment) { name =>
          get {
            onComplete(ServerDAO.getServerByName(name)) {
              case Success(Some(s)) => complete(s)
              case Success(None)    => complete(StatusCodes.NotFound -> s"Server with name $name not found")
              case Failure(ex)      => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
            }
          }
        }
      } ~
      path("users" / IntNumber / "servers") { user_id =>
        get {
          complete(UserServerDAO.getAllServerFromUser(user_id))
        }
      }
    }
}