package com.cytech.myakka
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route

import scala.concurrent.Future
import com.cytech.myakka.MessageActor
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



trait MessageJsonFormats extends DefaultJsonProtocol {
  given messageFormat: RootJsonFormat[Message] = jsonFormat3(Message.apply)
  given actionPerformedFormat: RootJsonFormat[ActionPerformed] = jsonFormat1(ActionPerformed.apply)
}

//#import-json-formats
class Routes(roomActor: ActorRef[Command], auth: BasicAuthConfig)(implicit val system: ActorSystem[_]) extends MessageJsonFormats {


  // If ask takes more time than this to complete the request is failed
  private given Timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))


  def sendMessage(message: Message): Future[ActionPerformed] =
    roomActor.ask(ProduceMessage(message, _))

  def myUserPassAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if id == auth.user && p.verify(auth.password) => Some(id)
      case _ => None
    }
  }
  val route: Route  = cors() {
    pathPrefix("message") {
      authenticateBasic(realm = "secure  site", myUserPassAuthenticator) { _ =>
          pathEnd {
              post {
                entity(as[Message]) { message =>
                  onSuccess(sendMessage(message)) { performed =>
                    complete(StatusCodes.Created , performed)
                  }
                }
              }
    }
      }
}
  }
}
