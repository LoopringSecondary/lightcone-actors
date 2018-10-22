import sbt._
import Keys._
import Settings._
import Dependencies._

lazy val core = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(
    basicSettings,
    libraryDependencies ++= commonDependency)
