import com.atlassian.labs.gitstamp.GitStampPlugin._

Seq( gitStampSettings: _* )

name := """archetypes"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.4"

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
  "org.zeroturnaround" % "zt-zip" % "1.8",
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "jquery" % "1.10.2-1",
  "org.webjars" % "bootstrap" % "3.3.2",
  "org.webjars" % "bootstrap-material-design" % "0.2.1",
  "org.webjars" % "font-awesome" % "4.3.0-1",
  "org.kohsuke" % "github-api" % "1.61",
  "com.mohiva" %% "play-silhouette" % "1.0",
  cache,
  ws,
  filters
)

scalacOptions ++= Seq(
  "-deprecation",            // Emit warning and location for usages of deprecated APIs.
  "-feature",                // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",              // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings",        // Fail the compilation if there are any warnings.
  "-Xlint",                  // Enable recommended additional warnings.
  "-Ywarn-adapted-args",     // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code",        // Warn when dead code is identified.
  "-Ywarn-inaccessible",     // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen"     // Warn when numerics are widened.
)
