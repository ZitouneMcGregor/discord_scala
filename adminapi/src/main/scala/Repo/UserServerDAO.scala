package Repo

import Models.UserServer
import utils.DatabaseConfig
import java.sql.PreparedStatement
import scala.compiletime.ops.int
import models.User
import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import models.Server
import scala.concurrent.ExecutionContext
import scala.concurrent.Future




object UserServerDAO{

    given ExecutionContext = ExecutionContext.global


    def insertUserServer(userServer: UserServer): Future[Boolean] = Future {
    val connection = DatabaseConfig.getConnection
    val countQuery = "SELECT COUNT(*) FROM SERVER_USER WHERE server_id=?"
    val insertQuery = "INSERT INTO SERVER_USER (user_id, server_id) VALUES (?, ?)"

    try {
        // Vérifier que `server_id` et `user_id` sont définis
        (userServer.server_id, userServer.user_id) match {
            case (Some(serverId), Some(userId)) =>
                // Vérifier le nombre d'utilisateurs sur ce serveur
                val countStatement: PreparedStatement = connection.prepareStatement(countQuery)
                countStatement.setInt(1, serverId)
                val resultSet: ResultSet = countStatement.executeQuery()

                if (resultSet.next() && resultSet.getInt(1) >= 10) {
                    println(" Limite d'utilisateurs atteinte pour le serveur ")
                    false
                } else {
                  val insertStatement: PreparedStatement = connection.prepareStatement(insertQuery)
                  insertStatement.setInt(1, userId)
                  insertStatement.setInt(2, serverId)

                  val rowsInserted = insertStatement.executeUpdate()
                  rowsInserted > 0
                }
            case _ =>
                println(" Error, server_id ou user_id est null")
                false
        }
    } catch {
        case e: Exception =>
            e.printStackTrace()
            false
    } finally {
        connection.close()
    }
}

    def deleteUserServer(userServer: UserServer): Future[Boolean] =Future {
        val connection = DatabaseConfig.getConnection
        val query = "DELETE FROM SERVER_USER WHERE user_id = ? AND server_id = ?"

        try{
            val statement: PreparedStatement = connection.prepareStatement(query)
            userServer.user_id match{
                case Some(value) => statement.setInt(1, value)
                case None => 
                    println("Error, server id is null") 
                    false
            }
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

    def getAllUserFromServer(server_id: Int): Future[List[User]] = Future{
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

      def getAllUserNotFromServer(server_id: Int): Future[List[User]] = Future{
        val connection = DatabaseConfig.getConnection
        val query = "SELECT u.* FROM USER u WHERE u.deleted = false AND u.id NOT IN (SELECT su.user_id FROM SERVER_USER su WHERE su.server_id = ?);"
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

    def getAllServerFromUser(user_id: Int): Future[List[Server]] = Future{
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

    def getUserServerByIds(user_id: Int,server_id: Int): Future[Option[UserServer]] = Future {
    val connection = DatabaseConfig.getConnection
    val query = "SELECT * FROM SERVER_USER WHERE user_id = ? AND server_id = ?"
    
    try {
      val statement = connection.prepareStatement(query)
      statement.setInt(1, user_id)
      statement.setInt(2,server_id)
      val resultSet = statement.executeQuery()

      if (resultSet.next()) {
        Some(UserServer(
          Some(resultSet.getInt("id")),
          Some(resultSet.getInt("user_id")),
          Some(resultSet.getInt("server_id")),
          Some(resultSet.getBoolean("admin"))
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
  
      def updateAdminByIds(user_id: Int, server_id: Int, admin: Boolean): Future[Boolean]= Future {
    val connection = DatabaseConfig.getConnection
    val query = "UPDATE SERVER_USER SET admin = ?  WHERE user_id= ? AND server_id = ?"
    var updated = false

    try {
      val statement = connection.prepareStatement(query)
      statement.setInt(1, user_id)
      statement.setInt(2,server_id)
      statement.setBoolean(3, admin)
      
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



        


    





