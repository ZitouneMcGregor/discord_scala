package fond2laclasse
import akka.actor.ActorSystem
import com.sksamuel.pulsar4s.akka.streams.source
import com.sksamuel.pulsar4s.{ConsumerConfig, MessageId, PulsarClient, Subscription, Topic}
import org.apache.pulsar.client.api.Schema
import org.mongodb.scala.*
import io.circe.parser.decode
import io.circe.generic.auto.*
import Helpers.*
import org.mongodb.scala.result.InsertOneResult
import zio.http.ChannelEvent.Read
import zio.http.WebSocketFrame
import zio.http.WebSocketChannel
import zio.* 

import java.time.Instant
import scala.util.control.NonFatal

  case class Message(id: String, timestamp: Instant, content: String, metadata: Map[String, String])

object consumer {

  type MessageToSend = (String, List[WebSocketChannel])

  private val sendQueue: Queue[MessageToSend] = Unsafe.unsafe { implicit u =>
    Runtime.default.unsafe.run(Queue.unbounded[MessageToSend]).getOrThrow()
  }

  private val processQueue: ZIO[Any, Nothing, Unit] =
    sendQueue.take.flatMap { case (msgStr, channels) =>
      println(s"[Consumer ZIO Processor] Dequeued message. Attempting to send to ${channels.size} channels.")
      ZIO.foreachPar(channels) { channel =>
        println(s"[Consumer ZIO Processor] Attempting to send to channel: $channel")
        channel.send(Read(WebSocketFrame.text(msgStr)))
          .tapError(e => ZIO.succeed(println(s"[Consumer ZIO Processor] ERROR sending to $channel: ${e.getMessage}")))
          .tap(_ => ZIO.succeed(println(s"[Consumer ZIO Processor] SUCCESS sending to $channel")))
          .ignore 
      }
      .ignore
    }.forever

  def main(args: Array[String]): Unit = {



      // Démarrage du serveur WebSocket (assure vous que le WS gère l'inscription avec la liste de serveurs)
      WebSocketServerAdvanced.startWsServer()

      // Initialisation MongoDB (optionnel)
      val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
      val database: MongoDatabase = mongoClient.getDatabase("discordMongo")
      val collection: MongoCollection[Document] = database.getCollection("messages")

      given system: ActorSystem = ActorSystem()

      given schema: Schema[Array[Byte]] = Schema.BYTES

      val client = PulsarClient("pulsar://localhost:6650")
      val topic = Topic("persistent://public/default/discord-messages")

      val consumer = client.consumer(ConsumerConfig(Subscription("mysub"), Seq(topic)))

      Unsafe.unsafe { implicit u =>
        Runtime.default.unsafe.run(processQueue.forkDaemon)
      }

      val control = source(() => consumer, Some(MessageId.latest))
        .map { msg =>
          val msgStr = new String(msg.data, "UTF-8")
          println(s"Message reçu: $msgStr")

          val doc: Document = Document(msgStr)
          val observable: Observable[InsertOneResult] = collection.insertOne(doc)
          observable.subscribe(new Observer[InsertOneResult] {
            override def onNext(result: InsertOneResult): Unit = println(result)

            override def onError(e: Throwable): Unit = println("Failed: " + e.getMessage)

            override def onComplete(): Unit = println("Completed")
          })

          decode[Message](msgStr) match {
            case Right(message) =>
                SubscriptionRegistry.getChannels(message.id) match {
                  case channels if channels.nonEmpty =>
                    val offerEffect = sendQueue.offer((msgStr, channels.toList))
                    Unsafe.unsafe { implicit u =>
                      Runtime.default.unsafe.run(offerEffect.ignore)
                    }
                  case _ =>
                    println(s"[Consumer Akka Stream] No channels found for ID ${message.id}, skipping enqueue.")
                }

            case Left(err: io.circe.Error) => 
              println(s"[Consumer Akka Stream] Error decoding message: ${err.getMessage}")
          }

        consumer.acknowledge(msg.messageId)
        () 
      }
      .run()
  }
}