name := "auction-bids"

organization := "online"

version := "0.0.1"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test" withSources() withJavadoc()
)

initialCommands := "import online.auctionbids._"

