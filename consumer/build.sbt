import sbt.Keys.libraryDependencies
import sbtassembly.AssemblyPlugin.autoImport._

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
      "com.clever-cloud.pulsar4s" %% "pulsar4s-circe" % pulsar4sVersion,
      ("org.mongodb.scala" %% "mongo-scala-driver" % "5.4.0").cross(CrossVersion.for3Use2_13),
      "dev.zio" %% "zio-http" % "3.2.0"
    )
  )

  assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("application.conf") => MergeStrategy.concat
  case _ => MergeStrategy.first
}