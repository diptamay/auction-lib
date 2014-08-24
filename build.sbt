name := "auction-bids"

organization := "online"

version := "0.0.1"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.5" withSources() withJavadoc(),
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test" withSources() withJavadoc(),
  "joda-time" % "joda-time"    % "2.4"
)

initialCommands := "import online.auctionbids._"

