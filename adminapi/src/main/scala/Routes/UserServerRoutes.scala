package routes

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import Models.UserServer
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import Repo.UserServerDAO
import org.apache.pekko.http.scaladsl.model.StatusCodes
import dao.UserDAO


trait UserServerJsonFormats extends DefaultJsonProtocol{
    implicit val userServerFormat:RootJsonFormat[UserServer] = jsonFormat3(UserServer.apply)
}

object UserServerRoutes extends UserServerJsonFormats{

    val route: Route =
                pathPrefix("server" / IntNumber){ id_server =>
                path("userServer"){
                    post{
                        entity(as[UserServer]) { userServer =>
                            val userServerWithIdServer = userServer.copy(server_id = Some(id_server))
                            if(UserServerDAO.insertUserServer(userServerWithIdServer)){
                                complete(StatusCodes.Created -> "User added to the server successfully")
                            
                            }else{
                                complete(StatusCodes.InternalServerError -> "Error while inserting the user to the server")
                            }

                        }
                    } ~
                    delete{
                        entity(as[UserServer]){ userServer =>
                            val userServerWithIdServer = userServer.copy(server_id = Some(id_server))
                            if(UserServerDAO.deleteUserServer(userServerWithIdServer)){
                                complete(StatusCodes.OK -> "User deleted from the server successfully")
                            }else{
                                complete(StatusCodes.InternalServerError ->"Error while deleting the user from the server")
                            }
                        }
                    } 

                }
        }

    }