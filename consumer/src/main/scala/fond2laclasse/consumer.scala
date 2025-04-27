package fond2laclasse

import akka.actor.ActorSystem
import com.sksamuel.pulsar4s.akka.streams.source
import com.sksamuel.pulsar4s.{ConsumerConfig, MessageId, PulsarClient, Subscription, Topic}
import org.apache.pulsar.client.api.Schema
import org.mongodb.scala.*
import io.circe.parser.decode
import io.circe.generic.auto.*
import Helpers.* // Assuming this provides necessary helpers
import org.mongodb.scala.result.InsertOneResult
import zio.http.ChannelEvent.Read // Import Read from ChannelEvent
import zio.http.WebSocketFrame // Import WebSocketFrame directly from zio.http
import zio.http.WebSocketChannel // Import WebSocketChannel directly from zio.http
import zio.* // Import necessary ZIO components (Runtime, Unsafe, Queue, etc.)

import java.time.Instant
import scala.util.control.NonFatal

// Define the Message case class - this was missing or commented out
case class Message(id: String, timestamp: Instant, content: String, metadata: Map[String, String])

object consumer {

  // Define a type for the items we'll put in the queue:
  // Tuple of the raw message string and the list of channels to send to
  type MessageToSend = (String, List[WebSocketChannel])

  // Define and initialize the ZIO Queue.
  // Use Unsafe.unsafe to run the ZIO effect that creates the queue and get its result.
  private val sendQueue: Queue[MessageToSend] = Unsafe.unsafe { implicit u =>
    // Runtime.default.unsafe.run returns a ZIO.Exit. We need the successful value from it.
    Runtime.default.unsafe.run(Queue.unbounded[MessageToSend]).getOrThrow() // .getOrThrow will extract the Queue or rethrow any error
  }

  // ZIO program to process messages from the queue and send via WebSocket
  private val processQueue: ZIO[Any, Nothing, Unit] =
    sendQueue.take.flatMap { case (msgStr, channels) =>
      println(s"[Consumer ZIO Processor] Dequeued message. Attempting to send to ${channels.size} channels.")
      // Use foreachPar to potentially send to multiple channels concurrently
      ZIO.foreachPar(channels) { channel =>
        println(s"[Consumer ZIO Processor] Attempting to send to channel: $channel")
        // The actual send operation using the ZIO HTTP channel
        channel.send(Read(WebSocketFrame.text(msgStr))) // Use imported WebSocketFrame
          .tapError(e => ZIO.succeed(println(s"[Consumer ZIO Processor] ERROR sending to $channel: ${e.getMessage}")))
          .tap(_ => ZIO.succeed(println(s"[Consumer ZIO Processor] SUCCESS sending to $channel")))
          .ignore // Ignore individual send errors so one failing channel doesn't stop processing for others
      }
      .ignore // Ignore errors from the foreachPar itself, keeps the processor running
    }.forever // Keep processing queue items indefinitely

  def main(args: Array[String]): Unit = {

    // Démarrage du serveur WebSocket (assure vous que le WS gère l'inscription avec la liste de serveurs)
    // This should be started *before* you start processing messages that rely on its registry.
    WebSocketServerAdvanced.startWsServer()
    println("[Consumer Main] WebSocket server startup initiated.") // Added a log for confirmation

    // Initialisation MongoDB (optionnel)
    val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
    val database: MongoDatabase = mongoClient.getDatabase("discordMongo")
    val collection: MongoCollection[Document] = database.getCollection("messages")
    println("[Consumer Main] MongoDB client initialized.") // Added a log for confirmation

    given system: ActorSystem = ActorSystem() // Akka ActorSystem for Pulsar stream
    println("[Consumer Main] Akka ActorSystem created.") // Added a log

    given schema: Schema[Array[Byte]] = Schema.BYTES // Schema for Pulsar messages
    println("[Consumer Main] Pulsar Schema defined.") // Added a log

    val client = PulsarClient("pulsar://localhost:6650")
    val topic = Topic("persistent://public/default/discord-messages")
    println(s"[Consumer Main] Pulsar client and topic '$topic' initialized.") // Added a log

    val consumer = client.consumer(ConsumerConfig(Subscription("mysub"), Seq(topic)))
    println("[Consumer Main] Pulsar consumer created.") // Added a log

    // --- Start the ZIO queue processing fiber BEFORE starting the Akka Stream ---
    // This ensures the ZIO processor is ready to receive messages from the queue
    Unsafe.unsafe { implicit u =>
      // Fork the processQueue program to run in the background as a daemon fiber
      Runtime.default.unsafe.run(processQueue.forkDaemon)
      println("[Consumer Main] Started ZIO queue processing fiber (dequeuing from queue).")
    }
    // -------------------------------------------------------------------------

    println("[Consumer Main] Starting Akka Stream to consume from Pulsar...")

    // Define and run the Akka Stream consumer
    val control = source(() => consumer, Some(MessageId.latest))
      .map { msg =>
        val msgBytes = msg.data // Get the message data as bytes
        val msgStr = new String(msgBytes, "UTF-8") // Convert to String
        println(s"[Consumer Akka Stream] Message received from Pulsar: $msgStr")

        // MongoDB insert (can remain as Fire-and-forget)
        // Consider using BsonDocument.parse(msgStr) if your messages are guaranteed JSON
        val doc: Document = Document(msgStr)
        collection.insertOne(doc).subscribe(new Observer[InsertOneResult] {
          override def onNext(result: InsertOneResult): Unit = println("[Consumer MongoDB] Inserted document: " + result)
          override def onError(e: Throwable): Unit = println("[Consumer MongoDB] Insert failed: " + e.getMessage)
          override def onComplete(): Unit = println("[Consumer MongoDB] Insert completed.")
        })
        println("[Consumer Akka Stream] Triggered MongoDB insert.")

        decode[Message](msgStr) match { // Use the defined Message case class
          case Right(message) =>
            println(s"[Consumer Akka Stream] Successfully decoded message (ID: ${message.id}). Looking up channels...")

            val channels = SubscriptionRegistry.getChannels(message.id)
            println(s"[Consumer Akka Stream] Found ${channels.size} channels for ID '${message.id}'.") // Avoid printing channels list itself

            if (channels.nonEmpty) {
              // --- Instead of running ZIO effects here, enqueue the message and channels ---
              val offerEffect = sendQueue.offer((msgStr, channels.toList))
               Unsafe.unsafe { implicit u =>
                  // Run the queue offer effect using Unsafe.
                  // .ignore means if the offer fails (e.g., queue is full, which is unlikely for unbounded),
                  // we won't crash the Akka Stream, but the message might be lost for WS clients.
                  Runtime.default.unsafe.run(offerEffect.ignore)
               }
              println(s"[Consumer Akka Stream] Enqueued message for ZIO processing for ID: ${message.id}.")
              // ------------------------------------------------------------------------
            } else {
              println(s"[Consumer Akka Stream] No channels found for ID ${message.id}, skipping enqueue.")
            }

          case Left(err: io.circe.Error) => // Use err and specify the type io.circe.Error
            println(s"[Consumer Akka Stream] Error decoding message: ${err.getMessage}")
            // Consider logging the message content here too for debugging decoding issues: println(s"Message content: $msgStr")
        }

        // Acknowledge the Pulsar message AFTER you have finished processing it
        // (which now includes decoding and enqueueing for WS sending)
        consumer.acknowledge(msg.messageId)
        println(s"[Consumer Akka Stream] Acknowledged Pulsar message ${msg.messageId}.")
        () // Akka Streams map needs to return Unit or something, explicit () is clear
      }
      .run() // Run the Akka Stream

      // Note: The application's main thread might exit if the Akka System and ZIO Runtime
      // management aren't set up to keep the process alive. `forkDaemon` helps the ZIO
      // part stay alive, and the Akka Stream might keep the Akka System alive.
      // In a production app, you'd need proper lifecycle management (e.g., using ZIO App, Akka Management, etc.).
  }
}