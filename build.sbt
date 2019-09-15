import Settings.Version

name := "akka-typed-cluster-example"
version := "0.2"
scalaVersion := "2.13.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % Version.akka,
  "com.typesafe.akka" %% "akka-cluster-typed" % Version.akka,
  "com.typesafe.akka" %% "akka-slf4j" % Version.akka,
  "com.typesafe.akka" %% "akka-distributed-data" % Version.akka,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
// Workaround - https://github.com/sbt/sbt/issues/5075
classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars