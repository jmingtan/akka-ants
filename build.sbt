name := "akka-test"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.4"
libraryDependencies += "nl.grons" %% "metrics-scala" % "3.5.4_a2.3"

fork in run := true