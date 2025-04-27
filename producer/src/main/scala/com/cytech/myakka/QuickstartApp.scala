package com.cytech.myakka

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route
import com.cytech.myakka.configuration.BasicAuthConfig
import com.sksamuel.pulsar4s.PulsarClient
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorSystem, Scheduler}
import com.cytech.myakka.MessageActor
import org.apache.pekko.http.scaladsl.server.RouteConcatenation._
import scala.util.Failure
import scala.util.Success
import org.apache.pekko.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import org.apache.pekko.http.scaladsl.server.RouteConcatenation._enhanceRouteWithConcatenation

object QuickstartApp {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding = Http().newServerAt("0.0.0.0", 8081).bind(routes)
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
    val authConfig = BasicAuthConfig(config)
    val pulsarHost = sys.env.getOrElse("PULSAR_HOST", "localhost")
    val client = PulsarClient(s"pulsar://$pulsarHost:6650")
        
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      context.log.info("Starting MessageActor")
      val messageActor = context.spawn(MessageActor(client), "MessageActor")
      
      context.watch(messageActor)
      
      val routes = new Routes(messageActor, authConfig)(context.system)

      startHttpServer(routes.route)(context.system)
      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
//#main-class
