lazy val pekkoHttpVersion = "1.1.0"
lazy val pekkoVersion     = "1.1.2"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")
fork := true

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "cytech",
      scalaVersion := "3.3.5"
    )),
    name := "adminApi",
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "resources",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http-cors"         % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http"                % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json"     % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-actor-typed"         % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream"              % pekkoVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "ch.qos.logback"    % "logback-classic"           % "1.3.14",
      "com.zaxxer"        % "HikariCP"                  % "5.0.1",
      "mysql"             % "mysql-connector-java"      % "8.0.33"
    )
  )

import sbtassembly.AssemblyPlugin.autoImport._

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
  case PathList("google", "protobuf", xs @ _*)                  => MergeStrategy.first
  case x => 
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

