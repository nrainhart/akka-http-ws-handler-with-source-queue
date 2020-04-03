name := "ws-test"

version := "0.1"

scalaVersion := "2.13.1"

val akkaVersion = "2.6.4"
val akkaHttpVersion = "10.1.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor-typed_2.13" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime
)
