import sbt._, Keys._

object Boilerplate extends AutoPlugin {
  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.11.7",
    organization := "enkode.me",
    addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.12"),
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint", "-Xfatal-warnings", "-Yinline-warnings"),
    resolvers ++= Seq(
      "Websudos releases" at "https://dl.bintray.com/websudos/oss-releases/",
      "patriknw at bintray" at "http://dl.bintray.com/patriknw/maven"
    )
  )

  object Modules {
    object Versions {
      val µPickle = "0.3.6"
      val scalaJsReact = "0.10.2"
      val scalaJsDom = "0.8.2"

      val react = "0.14.3"

      val akkaStreams = "2.0-M2"
      val akka = "2.4.1"
      val akkaDataReplication = "0.11"

      val slf4j = "1.7.10"
      val logback = "1.1.2"
      val scalaXml = "1.0.4"

      val phantom = "1.11.0"

      val enumeratum = "1.3.4"

      val scalatest = "2.2.4"
    }

    private val v = Versions

    def akka(name: String, version: String = v.akka) = "com.typesafe.akka" %% s"akka-$name" % version
    lazy val akkaDataReplication = "com.github.patriknw" %% "akka-data-replication" % v.akkaDataReplication

    def slf4j(name: String) = "org.slf4j" % s"slf4j-$name" % v.slf4j

    lazy val slf4j_api = slf4j("api")
    lazy val logback = "ch.qos.logback" % "logback-classic" % v.logback

    lazy val scala_xml = "org.scala-lang.modules" %% "scala-xml" % v.scalaXml

    lazy val μPickle = "com.lihaoyi" %% "upickle" % v.µPickle

    lazy val logging = slf4j_api :: logback :: Nil

    lazy val enumeratum = "com.beachape" %% "enumeratum" % v.enumeratum

    lazy val scalatest =  "org.scalatest" %% "scalatest" % v.scalatest
  }
}