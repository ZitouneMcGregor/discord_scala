import sbt.Keys.mappings.*

lazy val pekkoHttpVersion = "1.1.0"
lazy val pekkoVersion     = "1.1.3"
lazy val pulsar4sVersion = "2.10.0"


scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

Compile / packageDoc / mappings := Seq()

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
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
    "ch.qos.logback" % "logback-classic" % "1.5.17",
    "com.clever-cloud.pulsar4s" %% "pulsar4s-core" % pulsar4sVersion,
    "com.clever-cloud.pulsar4s" %% "pulsar4s-circe" % pulsar4sVersion
  )
)
