import Common.{ playVersion, driverVersion }

lazy val playJson = Def.setting {
  val ver = playVersion.value

  val groupId = {
    if (ver startsWith "3.") {
      "org.playframework"
    } else {
      "com.typesafe.play"
    }
  }

  groupId %% "play-json" % ver
}

lazy val `play-json-compat` = project
  .in(file("compat"))
  .settings(
    name := "reactivemongo-play-json-compat",
    description := "Compatibility library between BSON/Play JSON",
    mimaPreviousArtifacts := Set.empty[ModuleID],
    Test / compile / scalacOptions ++= {
      if (scalaBinaryVersion.value == "3") {
        Seq("-Wconf:cat=deprecation&msg=.*jsObjectWrites.*:s")
      } else {
        Seq.empty
      }
    },
    Test / compile / scalacOptions := {
      val opts = (Test / compile / scalacOptions).value
      val v = scalaBinaryVersion.value

      if (v == "2.12" || v == "2.13") {
        opts.filter(o => o != "-Xfatal-warnings" && o != "-Werror")
      } else {
        opts
      }
    },
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.36" % Provided,
      "org.reactivemongo" %% "reactivemongo-bson-api" % driverVersion.value
    ),
    libraryDependencies += playJson.value.cross(CrossVersion.binary) % Provided,
    libraryDependencies += {
      val v = {
        if (scalaBinaryVersion.value startsWith "3") {
          "5.5.3"
        } else {
          "4.10.6"
        }
      }

      "org.specs2" %% "specs2-matcher-extra" % v % Test
    }
  )

lazy val root = (project in file("."))
  .settings(
    Release.settings ++ Seq(
      publish := ({}),
      publishTo := None,
      mimaPreviousArtifacts := Set.empty[ModuleID] // TODO
    )
  )
  .aggregate(`play-json-compat`)
