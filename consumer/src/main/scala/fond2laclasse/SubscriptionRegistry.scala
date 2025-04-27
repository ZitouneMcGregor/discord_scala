package fond2laclasse

import java.util.concurrent.CopyOnWriteArrayList
import scala.jdk.CollectionConverters._
import zio.http.WebSocketChannel
import scala.collection.immutable.Set

case class Subscription(channel: WebSocketChannel, servers: Set[String])

object SubscriptionRegistry {
  // Liste concurrente d'abonnements
  private val subscriptions = new CopyOnWriteArrayList[Subscription]()

  def add(subscription: Subscription): Unit = subscriptions.add(subscription)

  def remove(channel: WebSocketChannel): Unit =
    subscriptions.removeIf(_.channel == channel)

  def getChannels(id: String): Set[WebSocketChannel] = {
    subscriptions.asScala 
      .filter(_.servers.contains(id))
      .map(_.channel)
      .toSet
  }
}