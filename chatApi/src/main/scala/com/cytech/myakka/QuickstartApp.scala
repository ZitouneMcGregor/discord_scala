package com.cytech.myakka

import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route
import com.cytech.myakka.configuration.BasicAuthConfig
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.scaladsl. Behaviors
import org.apache.pekko.actor.typed.ActorSystem

import scala.util.Failure
import scala.util.Success
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}

object QuickstartApp {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding = Http().newServerAt("0.0.0.0", 8083).bind(routes)
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
    val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
    val database: MongoDatabase = mongoClient.getDatabase("discordMongo")
    val collection: MongoCollection[Document] = database.getCollection("messages")

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      context.log.info("Starting ChatActor")
      val chatActor = context.spawn(ChatActor(collection), "ChatActor")
      
      context.watch(chatActor)
      
      val routes = new Routes(chatActor, authConfig)(context.system)

      startHttpServer(routes.route)(context.system)
      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
//#main-class
