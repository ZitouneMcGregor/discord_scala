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
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.util.Timeout
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._
import spray.json.*
import spray.json.DefaultJsonProtocol._
import com.cytech.myakka.configuration.BasicAuthConfig
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import scala.concurrent.duration.DurationInt

trait UserServerJsonFormats extends DefaultJsonProtocol {
  given userServerFormat: RootJsonFormat[UserServer] = jsonFormat4(UserServer.apply)
}

class UserServerRoutes(userServerRegistry: ActorRef[UserServerRegistry.Command], auth: BasicAuthConfig)(implicit val system: ActorSystem[_]) extends UserServerJsonFormats {
  
  private given Timeout = Timeout(5.seconds)

  val userServerRoutes: Route =
  pathPrefix("servers" / IntNumber / "users") { serverId =>
    concat(
      get {
        val usersFuture: Future[UserServer] = userServerRegistry.ask(ref => GetUsersServer(ref, serverId))

        onSuccess(usersFuture) {
          case UserServer(id, user_id, server_id, admin) =>
            complete(StatusCodes.OK, UserServer(id, user_id, server_id, admin))
        }
      },
      post {
        entity(as[UserServer]) { userServer =>
          val addUserFuture: Future[ActionPerformed] = userServerRegistry.ask(ref => AddUserServer(ref, userServer))

          onSuccess(addUserFuture) {
            case ActionPerformed(success, message) if success =>
              complete(StatusCodes.Created, message)
            case ActionPerformed(_, message) =>
              complete(StatusCodes.BadRequest, message)
          }
        }
      }
    )
  }
 
}
