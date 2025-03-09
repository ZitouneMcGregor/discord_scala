package com.cytech.myakka.routes

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import scala.concurrent.Future
import com.cytech.myakka.registery.*
import com.cytech.myakka.registery.UserServerRegistry.*
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.util.Timeout
import org.apache.pekko.http.cors.scaladsl.CorsDirectives.*
import spray.json.*
import spray.json.DefaultJsonProtocol.*
import com.cytech.myakka.configuration.BasicAuthConfig
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*

final case class UpdateAdminRequest(admin: Boolean)

trait UserServerJsonFormats extends DefaultJsonProtocol {
  given userServerFormat: RootJsonFormat[UserServer] = jsonFormat4(UserServer.apply)
  given userServersFormat: RootJsonFormat[UserServers] = jsonFormat1(UserServers.apply)
  given actionPerformedFormat: RootJsonFormat[ActionPerformed] = jsonFormat2(ActionPerformed.apply)
  given updateAdminRequestFormat: RootJsonFormat[UpdateAdminRequest] = jsonFormat1(UpdateAdminRequest.apply)
}

class UserServerRoutes(userServerRegistry: ActorRef[UserServerRegistry.Command], auth: BasicAuthConfig)(implicit val system: ActorSystem[_]) extends UserServerJsonFormats {
  
  private given Timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getUsers(serverId: Int): Future[UserServers] =
    userServerRegistry.ask(GetUsers(serverId, _))

  def addUser(userServer: UserServer): Future[ActionPerformed] =
    userServerRegistry.ask(AddUser(userServer, _))

  def removeUser(serverId: Int, userId: Int): Future[ActionPerformed] =
    userServerRegistry.ask(RemoveUser(serverId, userId, _))

  def updateAdmin(serverId: Int, userId: Int, admin: Boolean): Future[ActionPerformed] =
    userServerRegistry.ask(UpdateAdmin(serverId, userId, admin, _))

  // New method to get a specific user's data in a server
  def getUserInServer(serverId: Int, userId: Int): Future[Option[UserServer]] =
    userServerRegistry.ask(GetUserInServer(serverId, userId, _))

  def myUserPassAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if id == auth.user && p.verify(auth.password) => Some(id)
      case _ => None
    }
  }

  val userServerRoutes: Route = cors() {
    pathPrefix("servers" / IntNumber / "users") { serverId =>
      authenticateBasic(realm = "secure site", myUserPassAuthenticator) { _ =>
        concat(
          pathEnd {
            concat(
              get {
                onSuccess(getUsers(serverId)) { users =>
                  complete(StatusCodes.OK, users)
                }
              },
              post {
                entity(as[UserServer]) { userServer =>
                  if (userServer.server_id != serverId) {
                    complete(StatusCodes.BadRequest, "Server ID in path must match request body")
                  } else {
                    onSuccess(addUser(userServer)) { performed =>
                      complete(if (performed.success) StatusCodes.Created else StatusCodes.BadRequest, performed)
                    }
                  }
                }
              }
            )
          },
          path(IntNumber) { userId =>
            concat(
              get { // New route for specific user
                onSuccess(getUserInServer(serverId, userId)) {
                  case Some(userServer) => complete(StatusCodes.OK, userServer)
                  case None => complete(StatusCodes.NotFound, s"User $userId not found in server $serverId")
                }
              },
              delete {
                onSuccess(removeUser(serverId, userId)) { performed =>
                  complete(if (performed.success) StatusCodes.OK else StatusCodes.BadRequest, performed)
                }
              },
              put {
                entity(as[UpdateAdminRequest]) { request =>
                  onSuccess(updateAdmin(serverId, userId, request.admin)) { performed =>
                    complete(if (performed.success) StatusCodes.OK else StatusCodes.BadRequest, performed)
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