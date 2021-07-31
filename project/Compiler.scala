import sbt.Keys._
import sbt._

object Compiler {
  val playLower = "2.5.0"
  val playUpper = "2.9.2"

  lazy val settings = Seq(
    scalaVersion := "2.12.14",
    crossScalaVersions := Seq("2.11.12", scalaVersion.value, "2.13.6"),
    ThisBuild / crossVersion := CrossVersion.binary,
    Compile / unmanagedSourceDirectories += {
      val base = (Compile / sourceDirectory).value

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => base / "scala-2.13+"
        case _                       => base / "scala-2.13-"
      }
    },
    Test / unmanagedSourceDirectories += {
      val base = (Test / sourceDirectory).value

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => base / "scala-2.13+"
        case _                       => base / "scala-2.13-"
      }
    },
    scalacOptions ++= Seq(
      "-encoding", "UTF-8", "-target:jvm-1.8",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-Xlint",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-g:vars"
    ),
    scalacOptions ++= {
      if (scalaBinaryVersion.value == "2.13") Nil
      else Seq(
        "-Xmax-classfile-name", "128",
        "-Ywarn-infer-any",
        "-Ywarn-unused",
        "-Ywarn-unused-import"
      )
    },
    Compile / scalacOptions ++= {
      if (scalaVersion.value != "2.11") Nil
      else Seq(
        "-Yconst-opt",
        "-Yclosure-elim",
        "-Ydead-code",
        "-Yopt:_"
      )
    },
    Compile / doc / scalacOptions := (scalacOptions in Test).value,
    Compile / console / scalacOptions ~= {
      _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
    },
    Test / console / scalacOptions ~= {
      _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
    },
    Compile / doc / scalacOptions ++= Seq(
      "-unchecked", "-deprecation",
      /*"-diagrams", */"-implicits", "-skip-packages", "samples") ++
      Opts.doc.title("ReactiveMongo Play JSON API") ++
      Opts.doc.version(Release.major.value)
  )
}
