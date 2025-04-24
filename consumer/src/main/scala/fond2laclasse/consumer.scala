package fond2laclasse
import akka.actor.ActorSystem
import com.sksamuel.pulsar4s.akka.streams.source
import com.sksamuel.pulsar4s.{ConsumerConfig, ConsumerMessage, MessageId, PulsarClient, Subscription, Topic}
import org.apache.pulsar.client.api.Schema
import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.*
import io.circe.parser.decode
import io.circe.generic.auto.*
import Helpers.*
import org.mongodb.scala.result.InsertOneResult
import zio.http.ChannelEvent.Read
import zio.http.WebSocketFrame
import zio.*
import zio.{Runtime, Unsafe}



import java.time.Instant

  case class Message(id: String, timestamp: Instant, content: String, metadata: Map[String, String])

object consumer {



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
                val channels = SubscriptionRegistry.getChannels(message.id)
                val sendEffects = ZIO.foreach(channels) { channel =>
                  channel.send(Read(WebSocketFrame.text(msgStr)))
                }
                Unsafe.unsafe { implicit u: Unsafe =>
                  Runtime.default.unsafe.run(sendEffects)
                }
            case Left(error) =>
              println(s"Erreur lors du décodage du message: $error")
          }
          
          consumer.acknowledge(msg.messageId)
        }.run()
    }
  }
