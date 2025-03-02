package models

case class PrivateChat(
    id: Option[Int],
    user_id_1: Int, 
    user_id_2: Int,
    delete_user_1: Option[Boolean] = Some(false),
    delete_user_2: Option[Boolean] = Some(false)
)
