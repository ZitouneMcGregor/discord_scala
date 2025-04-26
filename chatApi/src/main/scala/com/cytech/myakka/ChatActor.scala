package com.cytech.myakka

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Sorts.ascending

import java.time.Instant
import scala.collection.immutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class Message(id: String, timestamp: Instant, content: String, metadata: Map[String, String])
case class Messages(messages: immutable.Seq[Message])

sealed trait Command
final case class GetMessages(id: String, replyTo: ActorRef[Messages]) extends Command

object ChatActor {

  def apply(collection: MongoCollection[Document]): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val ec: ExecutionContextExecutor = context.executionContext

      Behaviors.receiveMessage {
        case GetMessages(id, replyTo) =>
          dbGetMessages(id, collection).onComplete {
            case Success(messages) => replyTo ! messages
            case Failure(ex) =>
              context.log.error("Failed to fetch messages", ex)
              replyTo ! Messages(immutable.Seq.empty)
          }
          Behaviors.same
      }
    }

  private def dbGetMessages(id: String, collection: MongoCollection[Document])(implicit ec: ExecutionContextExecutor): Future[Messages] = {
    collection
      .find(equal("id", id))
      .sort(ascending("timestamp"))
      .toFuture()
      .map { docs =>
        val messages = docs.map { doc =>
          Message(
            id = doc.getString("id"),
            timestamp = Instant.parse(doc.getString("timestamp")),
            content = doc.getString("content"),
            metadata = doc.get("metadata").map(_.asDocument().toMap.map {
              case (k, v) => k -> v.asString().getValue
            }).getOrElse(Map.empty)
          )
        }
        Messages(messages.toList)
      }
  }
}
