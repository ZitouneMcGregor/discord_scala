package routes

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import Models.UserServer
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import spray.json.*
import Repo.UserServerDAO
import org.apache.pekko.http.scaladsl.model.StatusCodes
import dao.UserDAO
import scala.util.{Success, Failure}


trait UserServerJsonFormats extends DefaultJsonProtocol{
    given userServerFormat:RootJsonFormat[UserServer] = jsonFormat3(UserServer.apply)
}

object UserServerRoutes extends UserServerJsonFormats{

    val route: Route =
                pathPrefix("server" / IntNumber){ id_server =>
                path("userServer"){
                    post{
                        entity(as[UserServer]) { userServer =>
                            val userServerWithIdServer = userServer.copy(server_id = Some(id_server))
                            onComplete(UserServerDAO.insertUserServer(userServerWithIdServer)){
                               case Success(true) => complete(StatusCodes.Created -> "User added to the server successfully")
                            
                            
                               case Success(false) => complete(StatusCodes.InternalServerError -> "Error while inserting the user to the server in the db")

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
                    } 

                }
        }

    }