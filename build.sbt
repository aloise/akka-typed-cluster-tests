import Settings.Version

name := "akka-typed-cluster-example"
version := "0.1"
scalaVersion := "2.13.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % Version.akka,
  "com.typesafe.akka" %% "akka-distributed-data" % Version.akka
)