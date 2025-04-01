package fond2laclasse
import akka.actor.ActorSystem
import com.sksamuel.pulsar4s.akka.streams.source
import com.sksamuel.pulsar4s.{ConsumerConfig, ConsumerMessage, MessageId, PulsarClient, Subscription, Topic}
import org.apache.pulsar.client.api.Schema

import java.time.Instant

object consumer {

  def main(args: Array[String]): Unit = {

    case class Message(id: String, timestamp: Instant, content: String, metadata: Map[String, String])
    given system: ActorSystem = ActorSystem()
    given schema: Schema[Array[Byte]] = Schema.BYTES


    val client = PulsarClient("pulsar://localhost:6650")
    val topic = Topic("persistent://public/default/discord-messages")

    val consumer = client.consumer(ConsumerConfig(Subscription("mysub"), Seq(topic)))
    val control = source(() => consumer, Some(MessageId.latest))
      .map { msg =>
        println(new String(msg.data))
        consumer.acknowledge(msg.messageId)
      }.run()

  }
}
