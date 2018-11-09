import sbt._
import Keys._
import Settings._
import Dependencies._

lazy val actors = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(
    basicSettings,
    libraryDependencies ++= commonDependency,
    libraryDependencies ++= akkaDependency)
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen(flatPackage = false) -> (sourceManaged in Compile).value))

