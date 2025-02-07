package Repo

import Models.UserServer
import utils.DatabaseConfig
import java.sql.PreparedStatement
import scala.compiletime.ops.int
import models.User
import java.sql.ResultSet
import scala.collection.mutable.ListBuffer




object UserServerDAO{

    def insertUserServer(userServer: UserServer): Boolean= {
        val connection = DatabaseConfig.getConnection
        val query = "INSERT INTO SERVER_USER (user_id, server_id) VALUES (?, ?)"

        try{
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setInt(1, userServer.user_id)
            statement.setInt(2, userServer.server_id)

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
            statement.setInt(2, userServer.server_id)

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
    }


        


    





