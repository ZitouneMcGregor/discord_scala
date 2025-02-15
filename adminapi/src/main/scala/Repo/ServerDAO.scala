package dao

import java.sql.PreparedStatement
import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import models.Server
import utils.DatabaseConfig

object ServerDAO {
  def insertServer(server: Server): Boolean = {
    val connection = DatabaseConfig.getConnection
    val query = "INSERT INTO SERVER (name, img) VALUES (?, ?)"
    
    try {
      val statement: PreparedStatement = connection.prepareStatement(query)
      statement.setString(1, server.name)
      statement.setString(2, server.img)

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

  def getAllServer: List[Server] = {
    val connection = DatabaseConfig.getConnection
    val query = "SELECT * FROM SERVER"
    val server = ListBuffer[Server]()

    try {
      val statement = connection.createStatement()
      val resultSet: ResultSet = statement.executeQuery(query)

      while (resultSet.next()) {
        server += Server(
          Some(resultSet.getInt("id")),
          resultSet.getString("name"),
          resultSet.getString("img")
        )
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
    } finally {
      connection.close()
    }

    server.toList
  }

  def getServerById(id: Int): Option[Server] = {
    val connection = DatabaseConfig.getConnection
    val query = "SELECT * FROM SERVER WHERE id = ?"
    
    try {
      val statement = connection.prepareStatement(query)
      statement.setInt(1, id)
      val resultSet = statement.executeQuery()

      if (resultSet.next()) {
        Some(Server(
          Some(resultSet.getInt("id")),
          resultSet.getString("name"),
          resultSet.getString("img")
        ))
      } else {
        None
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        None
    } finally {
      connection.close()
    }
  }

  def updateServer(server: Server): Boolean = {
    val connection = DatabaseConfig.getConnection
    val query = "UPDATE SERVER SET name = ?, img = ? WHERE id = ?"
    
    try {
      val statement: PreparedStatement = connection.prepareStatement(query)
      statement.setString(1, server.name)
      statement.setString(2, server.img)

      server.id match{
        case Some(value) => statement.setInt(3, value)
        case None => 
          println("Error, id server is null")
          false
      }

      
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

  def deleteServer(id: Int): Boolean = {
    val connection = DatabaseConfig.getConnection
    val query = "DELETE FROM SERVER WHERE id = ?"
    
      try {
        val statement = connection.prepareStatement(query)
        statement.setInt(1, id)

        val rowsDeleted = statement.executeUpdate()
        rowsDeleted > 0
      } catch {
        case e: Exception =>
          e.printStackTrace()
          false
      } finally {
        connection.close()
      }
  }

  def getServerByName(name: String): Option[Server] = {
    val connection = DatabaseConfig.getConnection
    val query = "SELECT * FROM SERVER WHERE name = ?"
    
    try {
      val statement = connection.prepareStatement(query)
      statement.setString(1, name)
      val resultSet = statement.executeQuery()

      if (resultSet.next()) {
        Some(Server(
          Some(resultSet.getInt("id")),
          resultSet.getString("name"),
          resultSet.getString("img")
        ))
      } else {
        None
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        None
    } finally {
      connection.close()
    }
  }

}

