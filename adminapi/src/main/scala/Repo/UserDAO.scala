package dao

import java.sql.PreparedStatement
import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import models.User
import utils.DatabaseConfig

object UserDAO {
  def insertUser(user: User): Boolean = {
    val connection = DatabaseConfig.getConnection
    val query = "INSERT INTO USER (username, password) VALUES (?, ?)"
    
    try {
      val statement: PreparedStatement = connection.prepareStatement(query)
      statement.setString(1, user.username)
      statement.setString(2, user.password)

      val rowsInserted = statement.executeUpdate()
      rowsInserted > 0
    } catch {
      case e: Exception =>
        e.printStackTrace()
        false
    } finally {
      connection.close()
    }
  }

  def getAllUsers: List[User] = {
    val connection = DatabaseConfig.getConnection
    val query = "SELECT * FROM USER"
    val users = ListBuffer[User]()

    try {
      val statement = connection.createStatement()
      val resultSet: ResultSet = statement.executeQuery(query)

      while (resultSet.next()) {
        users += User(
          Some(resultSet.getInt("id")),
          resultSet.getString("username"),
          resultSet.getString("password")
        )
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
    } finally {
      connection.close()
    }

    users.toList
  }

}

