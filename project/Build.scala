import sbt._
import Keys._

object DemoBuild extends Build {

  val appName = "finagle-zipkin-demo"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.twitter" %% "finagle-core" % "6.8.0" withSources(),
    "com.twitter" %% "finagle-http" % "6.8.0" withSources(),
    "com.twitter" %% "finagle-serversets" % "6.8.0" withSources(),
    "com.twitter" %% "finagle-zipkin" % "6.8.0" withSources())

    
    
  val main = Project(id = appName, base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := appName,
      organization := "bwinparty",
      version := appVersion,
      scalaVersion := "2.9.2",
      libraryDependencies ++=appDependencies,
      resolvers += "twitter" at "http://maven.twttr.com/")
    )

  // resolvers += "twitter" at "http://maven.twttr.com/"
}
