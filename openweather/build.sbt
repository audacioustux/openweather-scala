val scala3Version = "3.4.0"
val AkkaVersion = "2.9.2"
val AkkaHttpVersion = "10.6.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "Openweather Kafka Producer",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    resolvers += "Akka library repository".at("https://repo.akka.io/maven"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "upickle" % "3.1.0",
      "com.typesafe.akka" %% "akka-stream-kafka" % "5.0.0",
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
      "ch.qos.logback" % "logback-classic" % "1.5.3"
    )
  )
  .enablePlugins(JavaAppPackaging)
