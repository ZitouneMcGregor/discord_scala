package com.cytech.myakka
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route

import scala.concurrent.Future
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.util.Timeout
import com.cytech.myakka.configuration.BasicAuthConfig
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import spray.json.*
import spray.json.DefaultJsonProtocol.*
import org.apache.pekko.http.cors.scaladsl.CorsDirectives.*

import java.time.Instant
import java.time.format.DateTimeFormatter



trait MessageJsonFormats extends DefaultJsonProtocol {

  implicit object InstantFormat extends RootJsonFormat[Instant] {
    private val formatter = DateTimeFormatter.ISO_INSTANT

    override def write(instant: Instant): JsValue =
      JsString(formatter.format(instant))

    override def read(json: JsValue): Instant = json match {
      case JsString(str) => Instant.parse(str)
      case other => deserializationError(s"Expected Instant as JsString, but got $other")
    }
  }

  implicit val messageFormat: RootJsonFormat[Message] = jsonFormat4(Message.apply)
  implicit val messageListFormat: RootJsonFormat[Messages] = jsonFormat1(Messages.apply)
}

//#import-json-formats
class Routes(ChatActor: ActorRef[Command], auth: BasicAuthConfig)(implicit val system: ActorSystem[_]) extends MessageJsonFormats {


  // If ask takes more time than this to complete the request is failed
  private given Timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))


  private def getMessages(id: String): Future[Messages] =
    ChatActor.ask(GetMessages(id, _))

  def myUserPassAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if id == auth.user && p.verify(auth.password) => Some(id)
      case _ => None
    }
  }
  val route: Route  = cors() {
    pathPrefix("server" / Segment / "room" / Segment / "messages") { (_, roomId) =>
      get(complete(getMessages(roomId)))
    } ~
      pathPrefix("privateChat" / Segment / "messages") { chatId =>
        get(complete(getMessages(chatId)))
      }
  }
}
