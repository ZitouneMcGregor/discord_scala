package com.cytech.myakka.routes

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route

import scala.concurrent.Future
import com.cytech.myakka.registery.UserRegistry.*
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



trait UserJsonFormats extends DefaultJsonProtocol {
  given userFormat: RootJsonFormat[User] = jsonFormat4(User.apply)
  given userListFormat2: RootJsonFormat[Users] = jsonFormat1(Users.apply)
  given actionPerformedFormat: RootJsonFormat[ActionPerformed] = jsonFormat2(ActionPerformed.apply)
}

final case class UpdateUserRequest(newUsername: String, newPassword: String)
object UpdateUserRequest {
  implicit val format: RootJsonFormat[UpdateUserRequest] =
    jsonFormat2(UpdateUserRequest.apply)
}

//#import-json-formats
//#user-routes-class
class UserRoutes(userRegistry: ActorRef[Command], auth: BasicAuthConfig)(implicit val system: ActorSystem[_]) extends UserJsonFormats {

  //#user-routes-class

  // If ask takes more time than this to complete the request is failed
  private given Timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers.apply)

  def getUser(username: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(username, _))
  
  def getUserById(id: Int): Future[GetUserResponse] =
    userRegistry.ask(GetUserById(id, _))

  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))

  def deleteUser(username: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(username, _))

  def updateUser(username: String, newUsername: String, newPassword : String): Future[ActionPerformed] =
    userRegistry.ask(UpdateUser(username,newUsername,newPassword,_))

  def searchUsers(prefix: String): Future[Users] =
    userRegistry.ask(SearchUsers(prefix, _))

  def myUserPassAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if id == auth.user && p.verify(auth.password) => Some(id)
      case _ => None
    }
  }

  //#all-routes
  val userRoutes: Route  = cors() {
  pathPrefix("users") {
    authenticateBasic(realm = "secure  site", myUserPassAuthenticator) { _ =>
      concat(
        pathEnd {
          concat(
            get {
              complete(getUsers())
            },
            post {
              entity(as[User]) { user =>
                onSuccess(createUser(user)) { performed =>
                  complete(StatusCodes.Created , performed)
                }
              }
            })
        },
        path(Segment) { username =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getUser(username)) { response =>
                  complete(response.maybeUser)
                }
              }
            },
            delete {
              onSuccess(deleteUser(username)) { performed =>
                complete(StatusCodes.OK, performed)
              }
            },
            put {
            entity(as[UpdateUserRequest]) { req =>
              onSuccess(updateUser(username, req.newUsername, req.newPassword)) { performed =>
                complete(StatusCodes.OK, performed)
              }
            }
          })
        },
        pathPrefix("id" / IntNumber) { id =>
            get {
              rejectEmptyResponse {
                onSuccess(getUserById(id)) { response =>
                  complete(response.maybeUser)
                }
              }
            }
          },
        path("search") {
            parameter("username".as[String]) { prefix =>
              get {
                onSuccess(searchUsers(prefix)) { users =>
                  complete(users) 
                }
              }
            }
          }
        
        
        )
    }
  }
}
}