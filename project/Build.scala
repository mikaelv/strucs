import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.strucs",
    version := "1.0.0",
    scalaVersion := "2.11.6",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq()
  )
}

object StrucsBuild extends Build {
  import BuildSettings._

  lazy val root: Project = Project(
    "strucs",
    file("."),
    settings = buildSettings
  ) aggregate(json, fix, core)

  lazy val json: Project = Project(
    "strucs-json",
    file("strucs-json"),
    settings = buildSettings
  ) dependsOn(core)

  lazy val fix: Project = Project(
    "strucs-fix",
    file("strucs-fix"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= scalaTest ++ joda
    )
  ) dependsOn(core)


  lazy val core: Project = Project(
    "strucs-core",
    file("strucs-core"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= scalaTest ++ Seq(
        "org.scala-lang" % "scala-reflect" % "2.11.6"
      ))
  )

  lazy val scalaTest = Seq("org.scalatest" % "scalatest_2.11" % "2.2.4" % "test")

  lazy val joda = Seq("joda-time" % "joda-time" % "2.8")
}