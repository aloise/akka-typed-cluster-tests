import Settings.Version

name := "akka-typed-cluster-example"
version := "0.3"
scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % Version.akka,
  "com.typesafe.akka" %% "akka-cluster-typed" % Version.akka,
  "com.typesafe.akka" %% "akka-slf4j" % Version.akka,
  "com.typesafe.akka" %% "akka-distributed-data" % Version.akka,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version.akka % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)
// Workaround - https://github.com/sbt/sbt/issues/5075
classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars