package routes

import models.User
import models.PrivateChat
import dao.PrivateChatDAO

import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import spray.json.*
import scala.util.{Success, Failure}
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import org.apache.pekko.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._
import org.apache.pekko.http.cors.scaladsl.settings.CorsSettings
import org.apache.pekko.http.cors.scaladsl.model.HttpOriginMatcher
import org.apache.pekko.http.scaladsl.model.HttpMethods._
import org.apache.pekko.http.cors.scaladsl.model.HttpHeaderRange
import scala.collection.immutable.Seq


trait PrivateChatJsonFormats extends DefaultJsonProtocol {
    given privateChatFormat: RootJsonFormat[PrivateChat] = jsonFormat5(PrivateChat.apply)
    given privateChatListFormat: RootJsonFormat[List[PrivateChat]] = listFormat(privateChatFormat)
}


object PrivateChatRoutes extends PrivateChatJsonFormats {

    val corsSettings: CorsSettings = CorsSettings.defaultSettings
    .withAllowedOrigins(HttpOriginMatcher.*)
    .withAllowedMethods(Seq(GET, POST, PUT, DELETE, OPTIONS))
    .withAllowedHeaders(HttpHeaderRange.*)

    val route: Route =
        cors(corsSettings) {  

        pathPrefix("privateChat") {
            pathEnd {
                post {
                    entity(as[PrivateChat]) { chat =>
                        onComplete(PrivateChatDAO.createPrivateChat(chat)){
                        case Success(true) => complete(StatusCodes.Created -> "Private chat created successfully")


                        case Success(false) => complete(StatusCodes.InternalServerError -> "Failed to create private chat in the db, maybe it already exists ")

                        case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
                        
                        }
                    }
                }
            } ~
                path("user" / IntNumber) { userId =>
                get {
                    onComplete(PrivateChatDAO.getAllPrivateChats(userId)) {
                        case Success(chats) if chats.nonEmpty => complete(chats)
                        case Success(_) => complete(StatusCodes.NotFound -> s"No private chats found for userId=$userId")

                        case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
                    }
                }
            } ~
                path(IntNumber / IntNumber) { (userId, chatId) =>
                delete {
                    onComplete(PrivateChatDAO.deletePrivateChatForUser(userId, chatId)) {
                        case Success(true) => complete(StatusCodes.OK -> "Chat updated as deleted for the user")

                        case Success(false) => complete(StatusCodes.InternalServerError -> "Failed to update chat as deleted, error in the db")
                        case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
                    }
                }
            }
        }
    }
}