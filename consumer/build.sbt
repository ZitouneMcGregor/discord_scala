lazy val pekkoHttpVersion = "1.1.0"
lazy val pekkoVersion     = "1.1.2"
lazy val pulsar4sVersion = "2.10.0"

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.cytech",
      scalaVersion    := "3.3.5"
    )),
    name := "consumer",
    libraryDependencies ++= Seq(
      "ch.qos.logback"    % "logback-classic"           % "1.5.18",
      "com.clever-cloud.pulsar4s" %% "pulsar4s-core" % pulsar4sVersion,
      "com.clever-cloud.pulsar4s" %% "pulsar4s-akka-streams" % pulsar4sVersion,
      "org.mongodb.scala" %% "mongo-scala-driver" % "5.3.0"

    )
  )
