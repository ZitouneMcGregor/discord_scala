package routes

import models.Server
import dao.ServerDAO

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import spray.json._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._


trait ServerJsonFormats extends DefaultJsonProtocol {
  implicit val serverFormat: RootJsonFormat[Server] = jsonFormat3(Server.apply)
  implicit val serverListFormat: RootJsonFormat[List[Server]] = listFormat(serverFormat)
}

object ServerRoutes extends ServerJsonFormats {
  val route: Route =
    path("server") {

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
    }
}
