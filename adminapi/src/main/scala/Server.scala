import models.User
import dao.UserDAO
import routes.UserRoutes
import org.apache.pekko
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.stream.Materializer

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object Server extends App {
  implicit val system: ActorSystem = ActorSystem("discord-api")
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContext = system.dispatcher

  val bindingFuture = Http().newServerAt("localhost", 8080).bind(UserRoutes.route)

  println("🚀 Server running at http://localhost:8080/")
  
}
