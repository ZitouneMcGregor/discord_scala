package Repo

import java.sql.PreparedStatement
import java.sql.ResultSet
import Models.Room
import utils.DatabaseConfig
import java.sql.PreparedStatement
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future




object RoomDAO{

    given ExecutionContext = ExecutionContext.global

    def insertRoom(room: Room): Future[Boolean] = { Future{
        val connection = DatabaseConfig.getConnection
        val query = "INSERT INTO ROOM (name, id_server) VALUES (?, ?)"
    
        try {
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setString(1, room.name)

            room.id_server match{

                case Some(value) =>  statement.setInt(2, value)
                case None => 
                    println("Error, server id is null")
                    false


            }

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
}

    def updateRoom(room: Room): Future[Boolean] = { Future{

        val connection = DatabaseConfig.getConnection
        val query = "UPDATE ROOM SET NAME = ? WHERE id = ?"
    
        try {
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setString(1, room.name)
            room.id match{

                case Some(value) =>  statement.setInt(2, value)
                case None => 
                    println("Error, id is null")
                    false


            }           

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
}

    def deleteRoom(room: Room): Future[Boolean] = { Future{
        val connection = DatabaseConfig.getConnection
        val query = "DELETE FROM ROOM WHERE id = ?"

        try{
            val statement: PreparedStatement = connection.prepareStatement(query)
            room.id match{

                case Some(value) =>  statement.setInt(1, value)
                case None =>
                    println("Error, id is null")
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
    }

    def getAllRoom(id_server: Int): Future[List[Room]] = { Future{
        val connection = DatabaseConfig.getConnection
        val query = "SELECT * FROM ROOM WHERE id_server = ?" 
        val rooms = ListBuffer[Room]()

        try {
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setInt(1, id_server)
            val resultSet: ResultSet = statement.executeQuery()

            while (resultSet.next()) {
            rooms += Room(
            Some(resultSet.getInt("id")),
            resultSet.getString("name"),
            Some(resultSet.getInt("id_server"))
        )
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
    } finally {
      connection.close()
    }

        rooms.toList
  }
}
}
