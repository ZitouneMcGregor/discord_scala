package Routes

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import Models.Room
import org.apache.pekko.http.scaladsl.server.Route
import Repo.RoomDAO
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.apache.pekko.http.scaladsl.model.StatusCodes



trait RoomJsonFormats extends DefaultJsonProtocol{
    implicit val roomFormat: RootJsonFormat[Room] = jsonFormat3(Room.apply)
    implicit val roomListFormat: RootJsonFormat[List[Room]] = listFormat(roomFormat)

}

object RoomRoutes extends RoomJsonFormats{
    val route: Route =
        pathPrefix("server" / IntNumber){ id_server =>
        path("rooms"){
            post{
                entity(as[Room]){ room =>
                    val roomWithIdServer = room.copy(id_server = Some(id_server))
                    if(RoomDAO.insertRoom(roomWithIdServer)){
                        complete(StatusCodes.Created -> "Room added successfully")
                    }else{
                        complete(StatusCodes.InternalServerError -> "Error while inserting the room")
                    }
                    
                }
            } ~
            put{
            entity(as[Room]){ room =>
                val roomWithIdServer = room.copy(id_server = Some(id_server))
                if(RoomDAO.updateRoom(roomWithIdServer)){
                    complete(StatusCodes.OK -> "Room updated successfully")
                }else{
                    complete(StatusCodes.InternalServerError -> "Error while updating the room")
                }

            }
        } ~
            delete{
            entity(as[Room]){ room =>
                val roomWithIdServer = room.copy(id_server = Some(id_server))
                if(RoomDAO.deleteRoom(roomWithIdServer)){
                    complete(StatusCodes.OK -> "Room deleted successfully")

                }else{
                    complete(StatusCodes.InternalServerError -> "Error while deleting the room")

                }

            }
        } ~
            get{
            complete(RoomDAO.getAllRoom(id_server))
        }
    
} 
        }

}