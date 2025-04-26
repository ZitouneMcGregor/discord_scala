package fond2laclasse
import zio.*

import zio.http.ChannelEvent.{ExceptionCaught, Read}
import zio.http._
import zio.http.codec.PathCodec.string



object WebSocketServerAdvanced {

  private val socketApp: WebSocketApp[Any] =
    Handler.webSocket { channel =>
      channel.receiveAll {
        case Read(WebSocketFrame.Text("end")) =>
          SubscriptionRegistry.remove(channel)
          channel.shutdown

        case Read(WebSocketFrame.Text(serversStr)) =>
          // Mise à jour / inscription
          val servers = serversStr.split(",").map(_.trim).toSet
          SubscriptionRegistry.add(Subscription(channel, servers))
          channel.send(Read(WebSocketFrame.text(s"Registered with servers: ${servers.mkString(", ")}")))

        case Read(WebSocketFrame.Close(status, reason)) =>
          // Fermeture propre du canal
          SubscriptionRegistry.remove(channel)
          Console.printLine(s"Closing channel with status: $status and reason: $reason")

        case ExceptionCaught(cause) =>
          Console.printLine(s"Channel error!: ${cause.getMessage}")

        case _ =>
          ZIO.unit
      }
    }


  private val routes: Routes[Any, Response] =
    Routes(
      Method.GET / "greet" / string("name") -> handler { (name: String, _: Request) =>
        Response.text(s"Greetings $name!")
      },
      Method.GET / "subscriptions"          -> handler(socketApp.toResponse),
    )

  def startWsServer(): Unit = {
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(Server.serve(routes).provide(Server.default).forkDaemon)
    }
    println("✅ WebSocket server started at ws://localhost:8080/subscriptions")
  }
}




