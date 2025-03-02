import sbt.Keys.mappings.*

lazy val pekkoHttpVersion = "1.1.0"
lazy val pekkoVersion     = "1.1.2"


scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

Compile / packageDoc / mappings := Seq()

lazy val DB_HOST=sys.env.get("POSTGRESQL_ADDON_HOST").getOrElse("localhost")
lazy val DB_PORT=sys.env.get("POSTGRESQL_ADDON_PORT").getOrElse("5432")
lazy val DB_NAME=sys.env.get("POSTGRESQL_ADDON_DB").getOrElse("AdminDiscord")
lazy val DB_USER=sys.env.get("POSTGRESQL_ADDON_USER").getOrElse("admin")
lazy val DB_PASS=sys.env.get("POSTGRESQL_ADDON_PASSWORD").getOrElse("1234567aA*")

flywayUrl := s"jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
flywayUser := DB_USER
flywayPassword := DB_PASS

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(FlywayPlugin)
  .settings(
  inThisBuild(
    List(organization := "com.cytech", scalaVersion := "3.3.5")
  ),
  name := "myakka",
  libraryDependencies ++= Seq(
    "org.apache.pekko" %% "pekko-http"                % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-http-cors"                % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-http-spray-json"     % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-actor-typed"         % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream"              % pekkoVersion,
    "ch.qos.logback" % "logback-classic" % "1.3.14",
    "org.tpolecat" %% "doobie-core" % "1.0.0-RC7",
    "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC7", 
  )
)
