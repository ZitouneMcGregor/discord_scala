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
import cats.syntax.all._

object PrivateChatRegistry {

  final case class PrivateChat(
    id: Option[Int] = None,
    user_id_1: Int, 
    user_id_2: Int,
    delete_user_1: Option[Boolean] = Some(false),
    delete_user_2: Option[Boolean] = Some(false)
  )
  final case class PrivateChats(privateChats: immutable.Seq[PrivateChat])

  sealed trait Command
  final case class GetPrivateChats(userId: Int, replyTo: ActorRef[PrivateChats]) extends Command
  final case class CreatePrivateChat(chat: PrivateChat, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetPrivateChat(id: Int, replyTo: ActorRef[GetPrivateChatResponse]) extends Command
  final case class DeletePrivateChatForUser(id: Int, userId: Int, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetPrivateChatResponse(maybeChat: Option[PrivateChat])
  final case class ActionPerformed(success: Boolean, description: String)

  def apply(pgConfig: PostgresConfig): Behavior[Command] =
    registry(transactor(pgConfig))

  type Transactor = doobie.Transactor.Aux[IO, Unit]
  private def transactor(pgConfig: PostgresConfig): Transactor = {
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = pgConfig.url,
      user = pgConfig.user,
      password = pgConfig.pass,
      logHandler = None
    )
  }

  private def dbGetPrivateChats(xa: Transactor, userId: Int): PrivateChats = {
    sql"""
         SELECT id, user_id_1, user_id_2, delete_user_1, delete_user_2
         FROM PRIVATE_CHAT
         WHERE (user_id_1 = $userId AND COALESCE(delete_user_1, false) = false)
            OR (user_id_2 = $userId AND COALESCE(delete_user_2, false) = false)
       """
      .query[PrivateChat]
      .to[List]
      .attempt
      .map {
        case Right(chats) => PrivateChats(chats)
        case Left(e) =>
          println(s"[DB ERROR] Failed to get private chats: ${e.getMessage}")
          PrivateChats(Seq.empty)
      }
      .transact(xa)
      .unsafeRunSync()
  }

  private def dbGetPrivateChat(xa: Transactor, chatId: Int): Option[PrivateChat] =
    sql"""
         SELECT id, user_id_1, user_id_2, delete_user_1, delete_user_2
         FROM PRIVATE_CHAT
         WHERE id = $chatId
       """
      .query[PrivateChat]
      .option
      .attempt
      .map {
        case Right(chatOpt) => chatOpt
        case Left(e) =>
          println(s"[DB ERROR] Failed to get private chat: ${e.getMessage}")
          None
      }
      .transact(xa)
      .unsafeRunSync()

  private def dbCreatePrivateChat(xa: Transactor, chat: PrivateChat): Boolean =
    sql"""
         INSERT INTO PRIVATE_CHAT (user_id_1, user_id_2, delete_user_1, delete_user_2)
         VALUES (${chat.user_id_1}, ${chat.user_id_2}, ${chat.delete_user_1.getOrElse(false)}, ${chat.delete_user_2.getOrElse(false)})
       """
      .update
      .run
      .attempt
      .map {
        case Right(_) => true
        case Left(e) =>
          println(s"[DB ERROR] Failed to create private chat: ${e.getMessage}")
          false
      }
      .transact(xa)
      .unsafeRunSync()

  private def dbDeletePrivateChat(xa: Transactor, chatId: Int, userId: Int): Boolean =
    sql"""
         UPDATE PRIVATE_CHAT
         SET delete_user_1 = CASE WHEN user_id_1 = $userId THEN true ELSE delete_user_1 END,
             delete_user_2 = CASE WHEN user_id_2 = $userId THEN true ELSE delete_user_2 END,
             updated_at     = NOW()
         WHERE id = $chatId
       """
      .update
      .run
      .attempt
      .map {
        case Right(rowsUpdated) => rowsUpdated > 0
        case Left(e) =>
          println(s"[DB ERROR] Failed to delete private chat for user: ${e.getMessage}")
          false
      }
      .transact(xa)
      .unsafeRunSync()

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetPrivateChats(userId, replyTo) =>
        replyTo ! dbGetPrivateChats(xa, userId)
        Behaviors.same

      case CreatePrivateChat(chat, replyTo) =>
        val ok = dbCreatePrivateChat(xa, chat)
        replyTo ! ActionPerformed(ok, if (ok) "PrivateChat created." else "Failed to create PrivateChat.")
        Behaviors.same

      case GetPrivateChat(chatId, replyTo) =>
        replyTo ! GetPrivateChatResponse(dbGetPrivateChat(xa, chatId))
        Behaviors.same

      case DeletePrivateChatForUser(chatId, userId, replyTo) =>
        val ok = dbDeletePrivateChat(xa, chatId, userId)
        replyTo ! ActionPerformed(ok, if (ok) s"PrivateChat($chatId) updated for user $userId." else s"Failed to update PrivateChat($chatId) for user $userId.")
        Behaviors.same
    }
}