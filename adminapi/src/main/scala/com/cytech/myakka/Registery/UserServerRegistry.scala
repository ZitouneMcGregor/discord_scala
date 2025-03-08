    package com.cytech.myakka.registery

    import org.apache.pekko.actor.typed.ActorRef
    import org.apache.pekko.actor.typed.Behavior
    import org.apache.pekko.actor.typed.scaladsl.Behaviors

    import scala.collection.immutable
    import doobie.*
    import doobie.implicits.*
    import cats.effect.IO
    import cats.effect.unsafe.implicits.global
    import com.cytech.myakka.configuration.PostgresConfig
    import doobie.postgres.sqlstate



    final case class UserServer(id: Option[Int], user_id: Option[Int], server_id: Option[Int], admin: Boolean)
    final case class UserServers(userServers: immutable.Seq[UserServer])



    object UserServerRegistry {
    sealed trait Command

    final case class GetUsersServer(replyTo: ActorRef[UserServer], idServer: Int) extends Command
    final case class AddUserServer(replyTo: ActorRef[ActionPerformed], userServer: UserServer) extends Command

    final case class GetServerResponse(maybeServer: Option[Server])
    final case class ActionPerformed(success: Boolean, description: String)

    def apply(pgConfig: PostgresConfig): Behavior[Command] = {
        registry(transactor(pgConfig))
    }

    type Transactor = doobie.Transactor.Aux[IO, Unit]
    def transactor(pgConfig: PostgresConfig): Transactor = {
        Transactor.fromDriverManager[IO](
        driver = "org.postgresql.Driver",
        url = pgConfig.url,
        user = pgConfig.user,
        password = pgConfig.pass,
        logHandler = None
        )
    }

    def dbGetUsersServer(xa: Transactor, idServer: Int): Option[UserServer] = {
            sql"SELECT * FROM server_user WHERE server_id=$idServer".
            query[UserServer].
            option.
            transact(xa).
            unsafeRunSync()
    }

    def dbInsertUserServer(xa: Transactor, userServer: UserServer): IO[Either[String, UserServer]] = {
        sql"INSERT INTO server_user (user_id, server_id, admin) VALUES (${userServer.user_id}, ${userServer.server_id}, ${userServer.admin})"
        .update
        .withUniqueGeneratedKeys[UserServer]("id", "user_id", "server_id", "admin")
        .attemptSomeSqlState {
            case sqlstate.class23.UNIQUE_VIOLATION => s"Error: Server '${userServer.server_id}' already exists."
        }
        .transact(xa)
    }


    private def registry(xa: Transactor): Behavior[Command] =
        Behaviors.receiveMessage {

            case GetUsersServer(replyTo, idServer) =>
                replyTo ! dbGetUsersServer(xa, idServer).getOrElse(UserServer(None, None, None, false))
                Behaviors.same

            case AddUserServer(replyTo, userServer) =>
                dbInsertUserServer(xa, userServer).unsafeRunSync() match {
                    case Right(insertUser) =>
                        replyTo ! ActionPerformed(true, s"User ${userServer.user_id} added to server ${userServer.server_id}")
                    case Left(errorMessage) =>
                        replyTo ! ActionPerformed(false, s"Failed to add user: $errorMessage")
                }
                Behaviors.same

            case _ => 
                println("Received an unknown command.")
                Behaviors.same
        }
    }
