package com.cytech.myakka.registery

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.collection.immutable
import doobie.*
import doobie.implicits.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cytech.myakka.configuration.PostgresConfig
import doobie.postgres.sqlstate

final case class PrivateChat(
    id: Option[Int],
    user_id_1: Int, 
    user_id_2: Int,
    delete_user_1: Option[Boolean],
    delete_user_2: Option[Boolean]
)
final case class PrivateChats(privateChats: immutable.Seq[PrivateChat])

object PrivateChatRegistry {

  final case class PrivateChat(id: Option[Int], user_id_1: Int, user_id_2: Int, delete_user_1: Option[Boolean], delete_user_2: Option[Boolean])
  final case class PrivateChats(privateChats: immutable.Seq[PrivateChat])

  sealed trait Command
  final case class GetPrivateChats(userId: Int, replyTo: ActorRef[PrivateChats]) extends Command
  final case class CreatePrivateChat(privateChat: PrivateChat, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetPrivateChat(id: Int, replyTo: ActorRef[GetPrivateChatResponse]) extends Command
  final case class DeletePrivateChatForUser(id: Int, userId: Int, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetPrivateChatResponse(maybePrivateChat: Option[PrivateChat])
  final case class ActionPerformed(success: Boolean, description: String)

  def apply(pgConfig: PostgresConfig): Behavior[Command] = {
    registry(transactor(pgConfig))
  }

  type Transactor = doobie.Transactor.Aux[IO, Unit]
  def transactor(pgConfig: PostgresConfig): Transactor = {
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = pgConfig.url,
      user = pgConfig.user,
      password = pgConfig.pass,
      logHandler = None
    )
  }

  def dbGetPrivateChats(xa: Transactor, userId: Int): PrivateChats = {
    PrivateChats(
      sql"""
        SELECT id, user_id_1, user_id_2, delete_user_1, delete_user_2
        FROM PRIVATE_CHAT
        WHERE (user_id_1 = $userId AND delete_user_1 = false)
          OR (user_id_2 = $userId AND delete_user_2 = false)
      """
      .query[PrivateChat]
      .to[List]
      .transact(xa)
      .unsafeRunSync()
    )
  }

  def dbGetPrivateChat(xa: Transactor, chatId: Int): Option[PrivateChat] = {
    sql"""
      SELECT id, user_id_1, user_id_2, delete_user_1, delete_user_2
      FROM PRIVATE_CHAT
      WHERE id = $chatId
    """
    .query[PrivateChat]
    .option
    .transact(xa)
    .unsafeRunSync()
  }

  def dbCreatePrivateChat(xa: Transactor, chat: PrivateChat): IO[Boolean] = {
    sql"""
      INSERT INTO PRIVATE_CHAT (user_id_1, user_id_2)
      VALUES (${chat.user_id_1}, ${chat.user_id_2})
    """
    .update
    .run
    .transact(xa)
    .map(_ > 0)
  }

  def dbDeletePrivateChat(xa: Transactor, chatId: Int, userId: Int): Boolean = {
    val rowsUpdated = sql"""
      UPDATE PRIVATE_CHAT
      SET 
        delete_user_1 = CASE WHEN user_id_1 = $userId THEN true ELSE delete_user_1 END,
        delete_user_2 = CASE WHEN user_id_2 = $userId THEN true ELSE delete_user_2 END
      WHERE id = $chatId
    """
    .update
    .run
    .transact(xa)
    .unsafeRunSync()
    rowsUpdated > 0
  }

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetPrivateChats(userId, replyTo) =>
        replyTo ! dbGetPrivateChats(xa, userId)
        Behaviors.same

      case CreatePrivateChat(chat, replyTo) =>
        val success = dbCreatePrivateChat(xa, chat).unsafeRunSync()
        if (success)
          replyTo ! ActionPerformed(true, "PrivateChat created.")
        else
          replyTo ! ActionPerformed(false, "Failed to create PrivateChat.")
        Behaviors.same

      case GetPrivateChat(chatId, replyTo) =>
        replyTo ! GetPrivateChatResponse(dbGetPrivateChat(xa, chatId))
        Behaviors.same

      case DeletePrivateChatForUser(chatId, userId, replyTo) =>
        val success = dbDeletePrivateChat(xa, chatId, userId)
        if (success)
          replyTo ! ActionPerformed(true, s"PrivateChat($chatId) updated for user $userId.")
        else
          replyTo ! ActionPerformed(false, s"Failed to update PrivateChat($chatId) for user $userId.")
        Behaviors.same
    }
}