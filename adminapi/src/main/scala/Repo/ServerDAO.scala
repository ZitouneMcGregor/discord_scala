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

}

