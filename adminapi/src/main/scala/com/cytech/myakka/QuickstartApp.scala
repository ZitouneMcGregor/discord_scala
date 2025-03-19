package com.cytech.myakka

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route
import com.cytech.myakka.configuration.{BasicAuthConfig, PostgresConfig}
import com.typesafe.config.ConfigFactory
import com.cytech.myakka.registery.{RoomRegistry, ServerRegistry, UserRegistry, PrivateChatRegistry}
import com.cytech.myakka.routes.{RoomRoutes, ServerRoutes, UserRoutes, PrivateChatRoutes}
import org.apache.pekko.http.scaladsl.server.RouteConcatenation._
import scala.util.Failure
import scala.util.Success
import org.apache.pekko.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import org.apache.pekko.http.scaladsl.server.RouteConcatenation._enhanceRouteWithConcatenation

object QuickstartApp {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
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

  def main(args: Array[String]): Unit = {
    // Load configuration
    val config = ConfigFactory.load("application.conf")
    val pgConfig = PostgresConfig(config)
    val authConfig = BasicAuthConfig(config)

    // UserRegistry, RoomRegistry, ServerRegistry and PrivateChatRegistry
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      context.log.info("Starting UserRegistry, RoomRegistry, ServerRegistry and PrivateChatRegistry")
      
      val userRegistryActor = context.spawn(UserRegistry(pgConfig), "UserRegistryActor")
      val roomRegistryActor = context.spawn(RoomRegistry(pgConfig), "RoomRegistryActor")
      val serverRegistryActor = context.spawn(ServerRegistry(pgConfig), "ServerRegistryActor")
      val privateChatRegistryActor = context.spawn(PrivateChatRegistry(pgConfig), "PrivateChatRegistryActor")
      
      context.watch(userRegistryActor)
      context.watch(roomRegistryActor)
      context.watch(serverRegistryActor)
      context.watch(privateChatRegistryActor)

      val userRoutes = new UserRoutes(userRegistryActor, authConfig)(context.system)
      val roomRoutes = new RoomRoutes(roomRegistryActor, authConfig)(context.system)
      val serverRoutes = new ServerRoutes(serverRegistryActor, authConfig)(context.system)
      val privateChatRoutes = new PrivateChatRoutes(privateChatRegistryActor, authConfig)(context.system)

      // Combine all routes
      val combinedRoutes: Route = userRoutes.userRoutes ~ roomRoutes.roomRoutes ~ serverRoutes.serverRoutes ~ privateChatRoutes.privateChatRoutes

      startHttpServer(combinedRoutes)(context.system)
      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
//#main-class
