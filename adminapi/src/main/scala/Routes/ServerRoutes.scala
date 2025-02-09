package routes

import models.Server
import dao.ServerDAO

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import spray.json._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import Repo.UserServerDAO


trait ServerJsonFormats extends DefaultJsonProtocol {
  implicit val serverFormat: RootJsonFormat[Server] = jsonFormat3(Server.apply)
  implicit val serverListFormat: RootJsonFormat[List[Server]] = listFormat(serverFormat)
}
object ServerRoutes extends ServerJsonFormats {
  val route: Route =
    pathPrefix("server") {
      pathEnd {
        post {
          entity(as[Server]) { server =>
            if (ServerDAO.insertServer(server)) {
              complete(StatusCodes.Created -> "Server inserted successfully")
            } else {
              complete(StatusCodes.InternalServerError -> "Error inserting Server")
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
            val updatedServer = server.copy(id = Some(id)) // Update the server with the id from the URL
            if (ServerDAO.updateServer(updatedServer)) {
              complete(StatusCodes.OK -> "Server updated successfully")
            } else {
              complete(StatusCodes.InternalServerError -> "Error updating Server")
            }
          }
        } ~
        delete {
          if (ServerDAO.deleteServer(id)) {
            complete(StatusCodes.OK -> s"Server with ID $id deleted successfully")
          } else {
            complete(StatusCodes.NotFound -> s"Server with ID $id not found")
          }
        }
      } ~
      path("search" / Segment) { name => // La requete doit être server/search/nom_du_server
        get {
          val server = ServerDAO.getServerByName(name)
          server match {
            case Some(s) => complete(s)
            case None    => complete(StatusCodes.NotFound -> s"Server with name $name not found")
          }
        }
      }
    }~
    path("users" / IntNumber / "servers") { user_id =>
      get {
        complete(UserServerDAO.getAllServerFromUser(user_id ))
      }

    }
}