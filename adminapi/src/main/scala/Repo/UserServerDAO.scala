package Repo

import Models.UserServer
import utils.DatabaseConfig
import java.sql.PreparedStatement
import scala.compiletime.ops.int
import models.User
import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import models.Server




object UserServerDAO{

    def insertUserServer(userServer: UserServer): Boolean= {
        val connection = DatabaseConfig.getConnection
        val query = "INSERT INTO SERVER_USER (user_id, server_id) VALUES (?, ?)"

        try{
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setInt(1, userServer.user_id)
            

            userServer.server_id match{
                case Some(value) => statement.setInt(2, value)
                case None => 
                    println("Error, server id is null") 
                    false
            }

            val rowsInserted = statement.executeUpdate()
            rowsInserted > 0

        }catch{
            case e:Exception =>
                e.printStackTrace()
                false
        }finally{
            connection.close()
        }
        }
        
    def deleteUserServer(userServer: UserServer): Boolean = {
        val connection = DatabaseConfig.getConnection
        val query = "DELETE FROM SERVER_USER WHERE user_id = ? AND server_id = ?"

        try{
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setInt(1, userServer.user_id)
            userServer.server_id match{
                case Some(value) => statement.setInt(2, value)
                case None => 
                    println("Error, server id is null") 
                    false
            }


            val rowsInserted = statement.executeUpdate()
            rowsInserted > 0

        }catch{
            case e:Exception =>
                e.printStackTrace()
                false
        }finally{
            connection.close()
        }
        }

    def getAllUserFromServer(server_id: Int): List[User] = {
        val connection = DatabaseConfig.getConnection
        val query = "SELECT u.* FROM USER u, SERVER_USER su WHERE u.id = su.user_id AND su.server_id = ?;"
        val users = ListBuffer[User]()

        try {
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setInt(1, server_id)
            val resultSet: ResultSet = statement.executeQuery()

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

    def getAllServerFromUser(user_id: Int): List[Server] = {
    val connection = DatabaseConfig.getConnection
    val query = "SELECT s.* FROM SERVER s, SERVER_USER su WHERE s.id = su.server_id AND su.user_id = ?;"
    val server = ListBuffer[Server]()

    try {
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setInt(1, user_id)
            val resultSet: ResultSet = statement.executeQuery()

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


        


    





