import sbt.*
import sbt.Keys.*

object Dependencies:

  object V:

    val cats            = "2.13.0"
    val catsEffect      = "3.7.1"
    val munit           = "1.3.3"
    val munitCatsEffect = "2.2.1"

  lazy val cats = libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core"   % V.cats,
    "org.typelevel" %% "cats-effect" % V.catsEffect % Test,
  )

  lazy val munit = libraryDependencies ++= Seq(
    "org.scalameta" %% "munit"             % V.munit           % Test,
    "org.typelevel" %% "munit-cats-effect" % V.munitCatsEffect % Test,
  )
