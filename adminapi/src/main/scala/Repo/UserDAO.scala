package dao

import java.sql.PreparedStatement
import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import models.User
import utils.DatabaseConfig
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object UserDAO {

  given ExecutionContext = ExecutionContext.global

  def insertUser(user: User): Future[Boolean] = Future {
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

  def getAllUsers: Future[List[User]] = Future{
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
          resultSet.getString("password"),
          Some(resultSet.getBoolean("deleted")),          
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

  def getUserByUsername(username: String): Future[Option[User]] = Future{
  val connection = DatabaseConfig.getConnection
  val query = "SELECT * FROM USER WHERE username = ?"
  var user: Option[User] = None

  try {
    val statement = connection.prepareStatement(query)
    statement.setString(1, username)
    val resultSet: ResultSet = statement.executeQuery()

    if (resultSet.next()) {
      user = Some(User(
        Some(resultSet.getInt("id")),
        resultSet.getString("username"),
        resultSet.getString("password"),
        Some(resultSet.getBoolean("deleted"))
      ))
    }
  } catch {
    case e: Exception =>
      e.printStackTrace()
  } finally {
    connection.close()
  }

  user
}

  def deleteUser(username: String): Future[Boolean] = Future{
  val connection = DatabaseConfig.getConnection
  val query = "UPDATE USER SET deleted = true WHERE username = ? AND deleted = false"
  var updated = false

  try {
  val statement = connection.prepareStatement(query)
  statement.setString(1, username)
  val rowsUpdated = statement.executeUpdate()

  updated = rowsUpdated > 0
  } catch {
  case e: Exception =>
    e.printStackTrace()
  } finally {
  connection.close()
  }

  updated
  }

  def updateUser(username: String, newUsername: String, newPassword: String): Future[Boolean]= Future {
    val connection = DatabaseConfig.getConnection
    val query = "UPDATE USER SET username = ?, password = ? WHERE username = ?"
    var updated = false

    try {
      val statement = connection.prepareStatement(query)
      statement.setString(1, newUsername)
      statement.setString(2, newPassword)
      statement.setString(3, username)
      
      val rowsUpdated = statement.executeUpdate()
      updated = rowsUpdated > 0
    } catch {
      case e: Exception =>
        e.printStackTrace()
    } finally {
      connection.close()
    }

    updated
  }


}

