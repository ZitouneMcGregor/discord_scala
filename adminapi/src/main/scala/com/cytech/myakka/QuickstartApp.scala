package com.cytech.myakka

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route
import com.cytech.myakka.configuration.{BasicAuthConfig, PostgresConfig}
import com.typesafe.config.ConfigFactory
import com.cytech.myakka.registery.* 
import com.cytech.myakka.routes.*
import scala.util.Failure
import scala.util.Success
import org.apache.pekko.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import org.apache.pekko.http.scaladsl.server.RouteConcatenation._enhanceRouteWithConcatenation

//#main-class
object QuickstartApp {
  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http().newServerAt("0.0.0.0", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
  //#start-http-server
  def main(args: Array[String]): Unit = {
    // configuration
    val config = ConfigFactory.load("application.conf")
    val pgConfig = PostgresConfig(config)
    val authConfig = BasicAuthConfig(config)

    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val userRegistryActor = context.spawn(UserRegistry(pgConfig), "UserRegistryActor")
      val serverRegistryActor = context.spawn(ServerRegistry(pgConfig), "ServerRegistryActor")
      val userServerRegistryActor = context.spawn(UserServerRegistry(pgConfig), "UserServerRegistryActor") 
 

      context.watch(userRegistryActor)
      context.watch(serverRegistryActor)
      context.watch(userServerRegistryActor)
      
      val routes = new UserRoutes(userRegistryActor, authConfig)(context.system)
      val serverRoutes = new ServerRoutes(serverRegistryActor, authConfig)(context.system) 
      val userServerRoutes = new UserServerRoutes(userServerRegistryActor, authConfig)(context.system) 

      startHttpServer(routes.userRoutes ~ serverRoutes.serverRoutes ~ userServerRoutes.userServerRoutes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
//#main-class