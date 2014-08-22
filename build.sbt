name := "Akka actorSelection overhead"

version := "1.0"
     
scalaVersion := "2.11.2"
     
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.5"
)

scalacOptions ++= Seq("-feature")
