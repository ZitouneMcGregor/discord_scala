

package com.cytech.myakka.routes

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route

import scala.concurrent.Future
import com.cytech.myakka.registery.ServerRegistry.*
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.util.Timeout
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._
import spray.json.*
import spray.json.DefaultJsonProtocol._
import com.cytech.myakka.configuration.BasicAuthConfig
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*


trait ServerJsonFormats extends DefaultJsonProtocol {
  given serverFormat: RootJsonFormat[Server] = jsonFormat3(Server.apply)
  given serverListFormat: RootJsonFormat[Servers] = jsonFormat1(Servers.apply)
  given actionPerformedFormat: RootJsonFormat[ActionPerformed] = jsonFormat2(ActionPerformed.apply)
  
}


class ServerRoutes(serverRegistry: ActorRef[Command], auth: BasicAuthConfig)(implicit val system: ActorSystem[_]) extends ServerJsonFormats {
  private given Timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getServers(): Future[Servers] = 
    serverRegistry.ask(GetServers.apply)

  def createServer(server: Server): Future[ActionPerformed] =
    serverRegistry.ask(CreateServer(server, _))

  def getServer(id: Int): Future[GetServerResponse] =
    serverRegistry.ask(GetServer(id, _))

  def deleteServer(id: Int): Future[ActionPerformed] =
    serverRegistry.ask(DeleteServer(id, _))

  def updateServer(id: Int, newName: String, newImg: String): Future[ActionPerformed] = 
    serverRegistry.ask(UpdateServer(id, newName, newImg, _))
  
  def myUserPassAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if id == auth.user && p.verify(auth.password) => Some(id)
      case _ => None
    }
   }

  val serverRoutes: Route = cors() {
    pathPrefix("servers") {
         authenticateBasic(realm = "secure  site", myUserPassAuthenticator) { _ =>
            concat(
                pathEnd {
                        concat(
                          get {
                            complete(getServers())
                          },
                          post {
                            entity(as[Server]) { server =>
                              onSuccess(createServer(server)) { performed =>
                                complete(StatusCodes.Created , performed)
                              }
                            }
                          })                  
                },
                path(Segment) { id =>
                  val maybeIntId = id.toIntOption
                  maybeIntId match {
                    case Some(intId) =>
                      concat(
                        get {
                          rejectEmptyResponse {
                            onSuccess(getServer(intId)) { response =>
                              complete(response.maybeServer)
                            }
                          }
                        },
                        delete {
                          onSuccess(deleteServer(intId)) { performed =>
                            complete(StatusCodes.OK, performed)
                          }
                        },
                        put {
                          entity(as[Server]) { server =>
                            maybeIntId match {
                              case Some(intId) =>
                                onSuccess(updateServer(intId, server.name, server.img.getOrElse(""))) { performed =>
                                  complete(StatusCodes.OK, performed)
                                }
                              case None =>
                                complete(StatusCodes.BadRequest, "Invalid server ID format")
                            }
                          }
                        }

                      )
                    case None =>
                      complete(StatusCodes.BadRequest, "Invalid server ID format")
                  }
                }

            )
         }
    }
  }
}
 