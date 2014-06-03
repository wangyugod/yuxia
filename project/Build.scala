import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "yuxia"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "com.typesafe.slick" %% "slick" % "2.0.0",
    "org.slf4j" % "slf4j-nop" % "1.6.4",
    //"com.typesafe.play" %% "play-slick" % "0.4.0",
    "mysql" % "mysql-connector-java" % "5.1.26",
    "org.apache.httpcomponents" % "httpclient" % "4.2.5",
    "commons-codec" % "commons-codec" % "1.9",
    "com.typesafe" %% "play-plugins-mailer" % "2.1-RC2",
    jdbc,
    cache
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
