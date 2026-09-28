import sbt.*
import sbt.Keys.*

/** External library dependencies. */
object Dependencies:

  /** The version to use for each dependency. */
  object V:

    val cats            = "2.13.0"
    val catsEffect      = "3.7.1"
    val munit           = "1.3.6"
    val munitCatsEffect = "2.2.1"

  /**
    * Library dependencies associated with cats, for the type classes a judge's
    * effect must have. Cats Effect is for tests alone, which judge in `IO`.
    */
  lazy val cats = libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core"   % V.cats,
    "org.typelevel" %% "cats-effect" % V.catsEffect % Test,
  )

  /** Library dependencies for testing with MUnit. */
  lazy val munit = libraryDependencies ++= Seq(
    "org.scalameta" %% "munit"             % V.munit           % Test,
    "org.typelevel" %% "munit-cats-effect" % V.munitCatsEffect % Test,
  )
