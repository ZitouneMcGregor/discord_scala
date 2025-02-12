package routes

import models.User
import models.PrivateChat
import dao.PrivateChatDAO

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import spray.json._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.apache.pekko.http.scaladsl.server.Directives._enhanceRouteWithConcatenation


trait PrivateChatJsonFormats extends DefaultJsonProtocol {
    implicit val privateChatFormat: RootJsonFormat[PrivateChat] = jsonFormat5(PrivateChat.apply)
    implicit val privateChatListFormat: RootJsonFormat[List[PrivateChat]] = listFormat(privateChatFormat)
}


object PrivateChatRoutes extends PrivateChatJsonFormats {
    val route: Route =

    pathPrefix("privateChat") {
        pathEnd {
            post {
                entity(as[PrivateChat]) { chat =>
                    if (PrivateChatDAO.createPrivateChat(chat)) {
                        complete(StatusCodes.Created -> "Private chat created successfully")
                    } else {
                        complete(StatusCodes.InternalServerError -> "Failed to create private chat, maybe it already exists ")
                    }
                }
            }
        } ~
        path("user" / IntNumber) { userId =>
            get {
                val chats = PrivateChatDAO.getAllPrivateChats(userId)
                if (chats.nonEmpty) {
                    complete(chats)
                } else {
                    complete(StatusCodes.NotFound -> s"No private chats found for userId=$userId")
                }
            }
        } ~
        path(IntNumber / IntNumber) { (userId, chatId) =>
            delete {
                if (PrivateChatDAO.deletePrivateChatForUser(userId, chatId)) {
                    complete(StatusCodes.OK -> "Chat updated as deleted for the user ")
                } else {
                    complete(StatusCodes.InternalServerError -> "Failed to update chat as deleted")
                }
            }
        }
    }
}
