import models.User
import dao.UserDAO
import routes.UserRoutes
import routes.PrivateChatRoutes
import models.Server
import dao.ServerDAO
import routes.ServerRoutes
import org.apache.pekko
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.stream.Materializer
import org.apache.pekko.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import routes.UserServerRoutes
import com.typesafe.config.ConfigFactory


import scala.concurrent.ExecutionContext
import scala.io.StdIn
import Routes.RoomRoutes

object Server extends App {
  implicit val system: ActorSystem = ActorSystem("discord-api")
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContext = system.dispatcher

  val config = ConfigFactory.load()
  val routes = UserRoutes.route ~ PrivateChatRoutes.route ~ UserServerRoutes.route ~ ServerRoutes.route ~ RoomRoutes.route

  val bindingFuture = Http().newServerAt("localhost", 8080).bind(routes)

  println("🚀 Server running at http://localhost:8080/")
  
}