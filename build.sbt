import com.atlassian.labs.gitstamp.GitStampPlugin._

Seq( gitStampSettings: _* )

name := """archetypes"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

scalacOptions += "-feature"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "net.codingwell" %% "scala-guice" % "4.0.0-beta5",
  "com.typesafe.play" %% "play-slick" % "0.8.1",
  "mysql" % "mysql-connector-java" % "5.1.32",
  "org.apache.maven" % "maven-artifact" % "3.2.5",
  "commons-io" % "commons-io" % "2.4",
  "com.mohiva" %% "play-html-compressor" % "0.4-SNAPSHOT",
  "eu.medsea.mimeutil" % "mime-util" % "2.1.3",
  "org.pegdown" % "pegdown" % "1.4.2",
  "org.jsoup" % "jsoup" % "1.8.1",
  "joda-time" % "joda-time" % "2.4",
  "org.joda" % "joda-convert" % "1.6",
  "com.jcabi" % "jcabi-manifests" % "1.1",
  cache,
  ws,
  filters
)
