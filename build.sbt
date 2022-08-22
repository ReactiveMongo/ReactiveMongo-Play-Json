import Common.{ playVersion, driverVersion }

lazy val playJson = Def.setting {
  "com.typesafe.play" %% "play-json" % playVersion.value
}

lazy val `play-json-compat` = project
  .in(file("compat"))
  .settings(
    name := "reactivemongo-play-json-compat",
    description := "Compatibility library between BSON/Play JSON",
    mimaPreviousArtifacts := Set.empty[ModuleID], // TODO
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "2.0.0" % Provided,
      "org.reactivemongo" %% "reactivemongo-bson-api" % driverVersion.value
    ),
    libraryDependencies ++= {
      if (scalaBinaryVersion.value == "3") {
        Seq(playJson.value.cross(CrossVersion.for3Use2_13) % Provided)
      } else {
        Seq(playJson.value.cross(CrossVersion.binary) % Provided)
      }
    },
    libraryDependencies ++= Seq(
      ("org.specs2" %% "specs2-matcher-extra" % "4.10.6")
        .cross(CrossVersion.for3Use2_13) % Test
    )
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
