package com.cytech.myakka

import com.cytech.myakka.MessageActor.produceMessage
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import io.circe.generic.auto.*

import java.time.Instant
import com.sksamuel.pulsar4s.{Producer, ProducerConfig, PulsarClient, Topic}
import com.sksamuel.pulsar4s.circe.*

case class Message(id: String, content: String, metadata: Map[String, String])
case class MessageSend(id: String, timestamp: Instant, content: String, metadata: Map[String, String])

sealed trait Command

final case class ProduceMessage(message: Message, replyTo: ActorRef[ActionPerformed]) extends Command

final case class ActionPerformed(description: String)


private def handle(producer: Producer[MessageSend]): Behavior[Command] =
  Behaviors.receiveMessage{
    case ProduceMessage(message,replyTo) =>
      replyTo ! ActionPerformed("Message envoyée dans la queu :)!")
        produceMessage(producer, message)
      Behaviors.same
  }
  


object MessageActor {

  def apply(client: PulsarClient): Behavior[Command] = {
    val topic = Topic("persistent://public/default/discord-messages")
    val producer = client.producer[MessageSend](ProducerConfig(topic))
    handle(producer)
  }


  def produceMessage(producer: Producer[MessageSend], message: Message): Unit = {
    val messageSend = MessageSend(message.id,Instant.now(),message.content,message.metadata)
    producer.send(messageSend)
  }


}