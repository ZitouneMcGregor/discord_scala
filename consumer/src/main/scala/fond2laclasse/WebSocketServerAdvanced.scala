package fond2laclasse

import zio.*
import zio.Console 

import zio.http._
import zio.http.ChannelEvent.{ExceptionCaught, Read}
import zio.http.WebSocketFrame
import zio.http.ChannelEvent
import zio.http.codec.PathCodec.string

object WebSocketServerAdvanced {

  private val socketApp: WebSocketApp[Any] =
    Handler.webSocket { channel =>
      channel.receiveAll {
        case Read(WebSocketFrame.Text("end")) =>
          SubscriptionRegistry.remove(channel)
          channel.shutdown

        case Read(WebSocketFrame.Text(serversStr)) =>
          val servers = serversStr.split(",").map(_.trim).toSet
          SubscriptionRegistry.add(Subscription(channel, servers))
          channel.send(Read(WebSocketFrame.text(s"Registered with servers: ${servers.mkString(", ")}"))).ignore
          Console.printLine(s"WS Server: Subscription added for $channel to ${servers.mkString(", ")}").ignore

        case Read(frame: WebSocketFrame.Text) =>
          val frameText = frame.text
          Console.printLine(s"WS Server: Received unexpected text frame from $channel: $frameText").ignore

        case Read(frame: WebSocketFrame) =>
           Console.printLine(s"WS Server: Received unexpected non-text frame type from $channel: ${frame.getClass.getSimpleName}").ignore

        case ChannelEvent.Registered => 
           Console.printLine(s"WS Server: Channel registered: $channel").ignore

        case ChannelEvent.Unregistered =>
          Console.printLine(s"WS Server: Channel unregistered: $channel").ignore
          ZIO.succeed(SubscriptionRegistry.remove(channel)) 

        

        case Read(WebSocketFrame.Close(status, reason)) =>
          Console.printLine(s"WS Server: Received Close frame from $channel. Status: $status, Reason: $reason").ignore *>
          ZIO.succeed(SubscriptionRegistry.remove(channel)) *>
          channel.shutdown

        case ExceptionCaught(cause) =>
          Console.printLine(s"WS Server: Exception caught on channel $channel: ${cause.getMessage}").ignore
          SubscriptionRegistry.remove(channel) // Ensure removal on error
          Console.printLine(s"WS Server: Channel $channel removed due to error.").ignore
          // channel.shutdown // Might already be shutting down due to the exception

        case other =>
          // Log any other ChannelEvent type you might not be explicitly handling
          Console.printLine(s"WS Server: Received unhandled channel event on $channel: ${other.getClass.getName}").ignore
          ZIO.unit // Ensure a ZIO effect resolving to Unit is returned for all cases
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
      val configLayer = Server.defaultWithPort(8082)
      // Use .ignore on the serve effect as the main thread doesn't need to await it
      Runtime.default.unsafe.run(Server.serve(routes).provide(configLayer).forkDaemon).ignore
    }
    println("✅ WebSocket server started at ws://localhost:8082/subscriptions")
  }
}