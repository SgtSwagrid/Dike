import IdeSettings.packagePrefix
import sbt._
import sbt.Keys._
import sbtunidoc.BaseUnidocPlugin.autoImport.*
import sbtunidoc.ScalaUnidocPlugin

// This build is developed as part of a larger private project,
// which includes it by reference and from which it is automatically synchronised.
// The project is named after the library, so that it doesn't clash with a host's own.

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

/**
  * Ranking items by judging them in pairs: the schedule of comparisons, the fit
  * of an ability to every item, and its calibration against items of known
  * score. Pure apart from the judge, which the host supplies in any effect.
  */
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
