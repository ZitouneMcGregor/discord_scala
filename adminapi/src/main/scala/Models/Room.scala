package models


case class Room(
    id: Option[Int],
    name: String,
    id_server: Option[Int]
)