package Models

case class UserServer(

    id: Option[Int],
    user_id: Option[Int],
    server_id: Option[Int],
    admin: Option[Boolean]
)
