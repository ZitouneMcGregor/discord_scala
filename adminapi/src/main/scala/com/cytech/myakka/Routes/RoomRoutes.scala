package com.cytech.myakka.routes

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import scala.concurrent.Future
import com.cytech.myakka.registery.RoomRegistry.*
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.util.Timeout
import com.cytech.myakka.configuration.BasicAuthConfig
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import spray.json.*
import spray.json.DefaultJsonProtocol._
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._

trait RoomJsonFormats extends DefaultJsonProtocol {
  given roomFormat: RootJsonFormat[Room] = jsonFormat3(Room.apply)
  given roomListFormat: RootJsonFormat[Rooms] = jsonFormat1(Rooms.apply)
  given actionPerformedFormat: RootJsonFormat[ActionPerformed] = jsonFormat2(ActionPerformed.apply)
}

class RoomRoutes(roomRegistry: ActorRef[Command], auth: BasicAuthConfig)(implicit val system: ActorSystem[_]) extends RoomJsonFormats {

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getRooms(): Future[Rooms] =
    roomRegistry.ask(GetRooms.apply)

  def getRoom(roomId: String): Future[GetRoomResponse] =
    roomRegistry.ask(GetRoom(roomId, _))

  def createRoom(room: Room): Future[ActionPerformed] =
    roomRegistry.ask(CreateRoom(room, _))

  def deleteRoom(roomId: String): Future[ActionPerformed] =
    roomRegistry.ask(DeleteRoom(roomId, _))

  def updateRoom(roomId: String, updatedRoom: Room): Future[ActionPerformed] =
    roomRegistry.ask(UpdateRoom(roomId, updatedRoom, _))

  def myUserPassAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if id == auth.user && p.verify(auth.password) => Some(id)
      case _ => None
    }
  }

  val roomRoutes: Route = cors() { 
    pathPrefix("rooms") {
      authenticateBasic(realm = "secure site", myUserPassAuthenticator) { _ =>
        concat(
          pathEnd {
            concat(
              get {
                complete(getRooms()) 
              },
              post {
                entity(as[Room]) { room =>
                  onSuccess(createRoom(room)) { performed =>
                    complete(StatusCodes.Created, performed) 
                  }
                }
              }
            )
          },
          path(Segment) { roomId =>
            concat(
              get {
                rejectEmptyResponse {
                  onSuccess(getRoom(roomId)) { response =>
                    complete(response.maybeRoom)
                  }
                }
              },
              delete {
                onSuccess(deleteRoom(roomId)) { performed =>
                  complete(StatusCodes.OK, performed) 
                }
              },
              put {
                entity(as[Room]) { room =>
                  onSuccess(updateRoom(roomId, room)) { performed =>
                    complete(StatusCodes.OK, performed)
                  }
                }
              }
            )
          }
        )
      }
    }
  }
}