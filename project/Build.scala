import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "yuxia"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
     // Add your project dependencies here,
    "com.typesafe.slick" % "slick-extensions_2.10" % "1.0.0",
    jdbc
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
