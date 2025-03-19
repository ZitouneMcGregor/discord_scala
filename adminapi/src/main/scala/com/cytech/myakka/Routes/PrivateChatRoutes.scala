package com.cytech.myakka.routes

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route

import scala.concurrent.Future
import scala.util.{Success, Failure}
import com.cytech.myakka.registery.PrivateChatRegistry.*
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.util.Timeout
import com.cytech.myakka.configuration.BasicAuthConfig
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._
import org.apache.pekko.http.cors.scaladsl.model.HttpOriginMatcher
import org.apache.pekko.http.scaladsl.model.HttpMethods._
import org.apache.pekko.http.cors.scaladsl.model.HttpHeaderRange
import spray.json.*
import spray.json.DefaultJsonProtocol._

import scala.collection.immutable.Seq


trait PrivateChatJsonFormats extends DefaultJsonProtocol {
    given privateChatFormat: RootJsonFormat[PrivateChat] = jsonFormat5(PrivateChat.apply)
    given privateChatListFormat: RootJsonFormat[List[PrivateChat]] = listFormat(privateChatFormat)
    given privateChatsFormat: RootJsonFormat[PrivateChats] = jsonFormat1(PrivateChats.apply)
    given actionPerformedFormat: RootJsonFormat[ActionPerformed] = jsonFormat2(ActionPerformed.apply)
}

class PrivateChatRoutes(privateChatRegistry: ActorRef[Command], authConfig: BasicAuthConfig)(implicit val system: ActorSystem[_]) extends PrivateChatJsonFormats {

    private given Timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

    def getPrivateChats(userId: Int): Future[PrivateChats] =
        privateChatRegistry.ask(GetPrivateChats(userId, _))

    def createPrivateChat(chat: PrivateChat): Future[ActionPerformed] =
        privateChatRegistry.ask(CreatePrivateChat(chat, _))

    def deletePrivateChat(chatId: Int, userId: Int): Future[ActionPerformed] =
        privateChatRegistry.ask(DeletePrivateChatForUser(chatId, userId, _))

    def myUserPassAuthenticator(credentials: Credentials): Option[String] = {
        credentials match {
            case p @ Credentials.Provided(id) if id == authConfig.user && p.verify(authConfig.password) => Some(id)
            case _ => None
        }
    }

    val privateChatRoutes: Route = cors() {
        pathPrefix("privateChat") {
        authenticateBasic(realm = "secure site", myUserPassAuthenticator) { _ =>
            concat(
            pathEnd {
                post {
                    entity(as[PrivateChat]) { chat =>
                        onSuccess(createPrivateChat(chat)) { performed =>
                        if (performed.success) {
                            complete(StatusCodes.Created -> performed)
                        } else {
                            complete(StatusCodes.InternalServerError -> performed)
                        }
                    }
                }
                }
            },

            path("user" / IntNumber) { userId =>
                get {
                onSuccess(getPrivateChats(userId)) { allChats =>
                    if (allChats.privateChats.nonEmpty) {
                        complete(allChats)
                    } else {
                        complete(StatusCodes.NotFound -> s"No private chats found for userId=$userId")
                    }
                }
                }
            },

            path(IntNumber / IntNumber) { (userId, chatId) =>
                delete {
                onSuccess(deletePrivateChat(chatId, userId)) { performed =>
                    if (performed.success) {
                        complete(StatusCodes.OK -> performed)
                    } else {
                        complete(StatusCodes.InternalServerError -> performed)
                    }
                }
                }
            }
            )
        }
        }
    }
}
