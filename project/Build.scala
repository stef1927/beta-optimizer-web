import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "dumb-trader"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Reactive Mongo dependencies
    "org.reactivemongo" %% "play2-reactivemongo" % "0.9",

    // WebJars pull in client-side web libraries
    "org.webjars" %% "webjars-play" % "2.1.0-3",
    "org.webjars" % "bootstrap" % "3.0.0",
    "org.webjars" % "knockout" % "2.3.0",
    "org.webjars" % "requirejs" % "2.1.8"
  
    // Add your own project dependencies in the form:
    // "group" % "artifact" % "version"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    scalaVersion := "2.10.2"
  )

}
