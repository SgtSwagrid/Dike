import IdeSettings.packagePrefix
import sbt._
import sbt.Keys._
import sbtunidoc.BaseUnidocPlugin.autoImport.*
import sbtunidoc.ScalaUnidocPlugin

val scala3 = "3.9.0"

ThisBuild / scalaVersion := scala3

ThisBuild / scalacOptions ++= Seq(
  "-explain",
  "-explain-types",
  "-explain-cyclic",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wunused:all",
)

// Named after the library so as not to clash with the projects of a build that
// includes this one by reference.
lazy val dike = project
  .in(file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    name          := "dike",
    packagePrefix := "com.alecdorrington.dike",
    Dependencies.cats,
    Dependencies.munit,
    ScalaUnidoc / unidoc / scalacOptions ++= Seq("-project", "Dike"),
  )
