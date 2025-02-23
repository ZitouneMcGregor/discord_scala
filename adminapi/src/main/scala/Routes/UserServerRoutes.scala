package routes

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import models.UserServer
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import spray.json.*
import Repo.UserServerDAO
import org.apache.pekko.http.scaladsl.model.StatusCodes
import dao.UserDAO
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._
import org.apache.pekko.http.cors.scaladsl.settings.CorsSettings
import org.apache.pekko.http.cors.scaladsl.model.HttpOriginMatcher
import org.apache.pekko.http.scaladsl.model.HttpMethods._
import org.apache.pekko.http.cors.scaladsl.model.HttpHeaderRange
import scala.collection.immutable.Seq

import scala.util.{Success, Failure}


trait UserServerJsonFormats extends DefaultJsonProtocol{
    given userServerFormat:RootJsonFormat[UserServer] = jsonFormat4(UserServer.apply)
}

object UserServerRoutes extends UserServerJsonFormats{

    val corsSettings: CorsSettings = CorsSettings.defaultSettings
    .withAllowedOrigins(HttpOriginMatcher.*)
    .withAllowedMethods(Seq(GET, POST, PUT, DELETE, OPTIONS))
    .withAllowedHeaders(HttpHeaderRange.*)

    val route: Route =
        cors(corsSettings) {
            pathPrefix("server" / IntNumber){ id_server =>
                path("userServer"){

                    post{
                        entity(as[UserServer]) { userServer =>
                            val userServerWithIdServer = userServer.copy(server_id = Some(id_server))
                            onComplete(UserServerDAO.insertUserServer(userServerWithIdServer)){
                               case Success(true) => complete(StatusCodes.Created -> "User added to the server successfully")
                            
                            
                               case Success(false) => 
                                    if (userServerWithIdServer.server_id.isDefined) {
                                                        complete(StatusCodes.BadRequest -> "Limite d'utilisateurs atteinte pour ce serveur")
                                                    } else {
                                                        complete(StatusCodes.InternalServerError -> "Error while inserting the user to the server in the db")
                                                    }
                               case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")

                            }

                        }
                    } ~
                    delete{
                        entity(as[UserServer]){ userServer =>
                            val userServerWithIdServer = userServer.copy(server_id = Some(id_server))
                            onComplete(UserServerDAO.deleteUserServer(userServerWithIdServer)){
                               case Success(true) => complete(StatusCodes.OK -> "User deleted from the server successfully")
                            
                               case Success(false) => complete(StatusCodes.InternalServerError ->"Error while deleting the user from the server in the db")

                               case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")

                            }
                        }
                    } ~
                    path("user" / IntNumber){id_user =>
                        get{
                            complete(UserServerDAO.getUserServerByIds(id_user,id_server))
                        }
                    }
                ~
                        path("user"/ IntNumber / "admin" / IntNumber){ (id_user,admin) =>
                            put{
                                val adminBool = admin match {
                                    case 1 => true
                                    case 0 => false
                                    case _ => throw new IllegalArgumentException("Invalid admin value")
                                }

                                onComplete(UserServerDAO.updateAdminByIds(id_user,id_server,adminBool)){
                                    case Success(true) => complete(StatusCodes.OK -> "Admin status updated successfully")
                                    case Success(false) => complete(StatusCodes.InternalServerError -> "Error while updating admin status in the db")
                                    case Failure(ex) => complete(StatusCodes.InternalServerError -> s"An error occurred: ${ex.getMessage}")
                                }
                            }
                        }

                    }

                }
            }
        }


            