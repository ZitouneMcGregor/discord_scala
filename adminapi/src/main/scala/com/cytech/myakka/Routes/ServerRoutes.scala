
package com.cytech.myakka.routes

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route

import scala.concurrent.Future
import com.cytech.myakka.registry.*
import com.cytech.myakka.registry.ServerRegistry.*
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


class ServerRoutes(serverRegistry: ActorRef[ServerRegistry.Command], auth: BasicAuthConfig)(implicit val system: ActorSystem[_]) extends ServerJsonFormats {
  private given Timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getServers(): Future[Servers] = 
    serverRegistry.ask(GetServers.apply)
  
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
                        get {
                            complete(getServers())
                        }
                    
                }
            )
         }
    }
  }
}
 