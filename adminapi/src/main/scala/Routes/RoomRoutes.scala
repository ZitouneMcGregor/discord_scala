package Routes

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import Models.Room
import org.apache.pekko.http.scaladsl.server.Route
import Repo.RoomDAO
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import org.apache.pekko.http.scaladsl.model.StatusCodes

import org.apache.pekko.http.cors.scaladsl.CorsDirectives._
import org.apache.pekko.http.cors.scaladsl.settings.CorsSettings
import org.apache.pekko.http.cors.scaladsl.model.HttpOriginMatcher
import org.apache.pekko.http.scaladsl.model.HttpMethods._
import org.apache.pekko.http.cors.scaladsl.model.HttpHeaderRange
import scala.collection.immutable.Seq

import scala.util.{Success, Failure}




trait RoomJsonFormats extends DefaultJsonProtocol{
    given roomFormat: RootJsonFormat[Room] = jsonFormat3(Room.apply)
    given roomListFormat: RootJsonFormat[List[Room]] = listFormat(roomFormat)

}

object RoomRoutes extends RoomJsonFormats{

    val corsSettings: CorsSettings = CorsSettings.defaultSettings
    .withAllowedOrigins(HttpOriginMatcher.*)
    .withAllowedMethods(Seq(GET, POST, PUT, DELETE, OPTIONS))
    .withAllowedHeaders(HttpHeaderRange.*)

    val route: Route =
        cors(corsSettings){
        pathPrefix("server" / IntNumber){ id_server =>
        path("rooms"){
            post{
                entity(as[Room]){ room =>
                    val roomWithIdServer = room.copy(id_server = Some(id_server))
                    onComplete(RoomDAO.insertRoom(roomWithIdServer)){
                      case Success(true) =>  complete(StatusCodes.Created -> "Room added successfully")
                    
                      case Success(false) => complete(StatusCodes.InternalServerError -> "Error while inserting the room in the db")

                      case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")

                    }
                    
                }
            } ~
            put{
            entity(as[Room]){ room =>
                val roomWithIdServer = room.copy(id_server = Some(id_server))
                onComplete(RoomDAO.updateRoom(roomWithIdServer)){
                 case Success(true) =>   complete(StatusCodes.OK -> "Room updated successfully")
                
                 case Success(false) =>  complete(StatusCodes.InternalServerError -> "Error while updating the room in the db")
                 
                case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")

                }
            }
            } ~
            delete{
            entity(as[Room]){ room =>
                val roomWithIdServer = room.copy(id_server = Some(id_server))
                onComplete(RoomDAO.deleteRoom(roomWithIdServer)){
                 case Success(true) =>   complete(StatusCodes.OK -> "Room deleted successfully")

                
                 case Success(false) =>  complete(StatusCodes.InternalServerError -> "Error while deleting the room in the db")

                 case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")

                }
            }
            } ~
            get{
            
            complete(RoomDAO.getAllRoom(id_server))
            }
        }
    }
}
}