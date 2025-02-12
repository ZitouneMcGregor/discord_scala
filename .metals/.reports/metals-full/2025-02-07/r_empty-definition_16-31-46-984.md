error id: username.
file://<WORKSPACE>/adminapi/src/main/scala/Repo/UserDAO.scala
empty definition using pc, found symbol in pc: username.
empty definition using semanticdb
|empty definition using fallback
non-local guesses:
	 -user/username.
	 -user/username#
	 -user/username().
	 -scala/Predef.user.username.
	 -scala/Predef.user.username#
	 -scala/Predef.user.username().

Document text:

```scala
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

  def getUserByUsername(username: String): Option[User] = {
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

def deleteUser(username: String): Boolean = {
  val connection = DatabaseConfig.getConnection
  val query = "UPDATE USER SET deleted = false WHERE username = ?"
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



}


```

#### Short summary: 

empty definition using pc, found symbol in pc: username.