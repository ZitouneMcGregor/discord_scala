package dao

import java.sql.PreparedStatement
import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import models.User
import models.PrivateChat
import utils.DatabaseConfig
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
object PrivateChatDAO {

  given ExecutionContext = ExecutionContext.global

  def createPrivateChat(chat: PrivateChat): Future[Boolean] = Future {
  val connection = DatabaseConfig.getConnection
  try {
    
    val checkSql = "SELECT id FROM PRIVATE_CHAT WHERE (user_id_1=? AND user_id_2=?)OR (user_id_1=? AND user_id_2=?)"

    val checkStmt = connection.prepareStatement(checkSql)
    checkStmt.setInt(1, chat.user_id_1)
    checkStmt.setInt(2, chat.user_id_2)
    checkStmt.setInt(3, chat.user_id_2)
    checkStmt.setInt(4, chat.user_id_1)
    val rs = checkStmt.executeQuery()

    if (rs.next()) {
      // si il existe deja
      val chatId = rs.getInt("id")

      val updateSql = "UPDATE PRIVATE_CHAT SET delete_user_1 = false, delete_user_2 = false WHERE id = ?"

      val updateStmt = connection.prepareStatement(updateSql)
      updateStmt.setInt(1, chatId)
      val updated = updateStmt.executeUpdate()
      updated > 0
    } else {
      // si ya pas
      val insertSql = "INSERT INTO PRIVATE_CHAT (user_id_1, user_id_2) VALUES (?, ?)"
      val insertStmt = connection.prepareStatement(insertSql)
      insertStmt.setInt(1, chat.user_id_1)
      insertStmt.setInt(2, chat.user_id_2)
      val rowsInserted = insertStmt.executeUpdate()
      rowsInserted > 0
    }
  } catch {
    case e: Exception =>
      e.printStackTrace()
      false
  } finally {
    connection.close()
  }
}



    def getAllPrivateChats(userId: Int): Future[List[PrivateChat]] = { Future{
        val connection = DatabaseConfig.getConnection
        val query = """SELECT * 
            FROM PRIVATE_CHAT
            WHERE (user_id_1 = ? AND delete_user_1 = false) 
                OR (user_id_2 = ? AND delete_user_2 = false)"""
        val chats = ListBuffer[PrivateChat]()

        println("alloaalloooo");

        try {
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setInt(1, userId)
            statement.setInt(2, userId)

            val resultSet: ResultSet = statement.executeQuery()

            while (resultSet.next()) {
                chats += PrivateChat(
                    Some(resultSet.getInt("id")),
                    resultSet.getInt("user_id_1"),
                    resultSet.getInt("user_id_2"),
                    Some(resultSet.getBoolean("delete_user_1")),
                    Some(resultSet.getBoolean("delete_user_2"))
                )
            }
        } catch {
            case e: Exception =>
                e.printStackTrace()
        } finally {
            connection.close()
        }

        chats.toList
    }
}

    def deletePrivateChatForUser(userId: Int, chatId: Int): Future[Boolean] = { Future{
        val connection = DatabaseConfig.getConnection
        val query ="""UPDATE PRIVATE_CHAT 
            SET delete_user_1 = CASE WHEN user_id_1 = ? THEN true ELSE delete_user_1 END,
                delete_user_2 = CASE WHEN user_id_2 = ? THEN true ELSE delete_user_2 END
            WHERE id = ?"""
        
        try {
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setInt(1, userId)
            statement.setInt(2, userId)
            statement.setInt(3, chatId)

            val rowsUpdated = statement.executeUpdate()
            rowsUpdated > 0
        } catch {
            case e: Exception =>
                e.printStackTrace()
                false
        } finally {
            connection.close()
        }
    }
}
}
