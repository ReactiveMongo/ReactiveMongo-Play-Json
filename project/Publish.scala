import sbt.Keys._
import sbt._

import com.typesafe.tools.mima.plugin.MimaKeys._

object Publish {

  lazy val settings = {
    val repoName = env("PUBLISH_REPO_NAME")
    val repoUrl = env("PUBLISH_REPO_URL")

    Seq(
      Compile / doc / scalacOptions ++= {
        if (scalaBinaryVersion.value startsWith "2.") {
          Seq( /*"-diagrams", */ "-implicits", "-skip-packages", "samples")
        } else {
          Seq("-skip-by-id:samples")
        }
      },
      Compile / doc / scalacOptions ++= Opts.doc.title(
        "ReactiveMongo Play JSON API"
      ) ++ Opts.doc.version(Release.major.value),
      // mimaDefaultSettings
      mimaFailOnNoPrevious := false,
      mimaPreviousArtifacts := {
        val v = scalaBinaryVersion.value

        if (v != "3" && v != "2.13") {
          Set(organization.value %% moduleName.value % Common.previousVersion)
        } else {
          Set.empty[ModuleID]
        }
      },
      publishMavenStyle := true,
      Test / publishArtifact := false,
      publishTo := Some(repoUrl).map(repoName at _),
      credentials += Credentials(
        repoName,
        env("PUBLISH_REPO_ID"),
        env("PUBLISH_USER"),
        env("PUBLISH_PASS")
      ),
      pomIncludeRepository := { _ => false },
      licenses := {
        Seq(
          "Apache 2.0" ->
            url("http://www.apache.org/licenses/LICENSE-2.0")
        )
      },
      homepage := Some(url("http://reactivemongo.org")),
      autoAPIMappings := true,
      pomExtra := (<scm>
          <url>git://github.com/ReactiveMongo/ReactiveMongo-Play-Json.git</url>
          <connection>scm:git://github.com/ReactiveMongo/ReactiveMongo-Play-Json.git</connection>
        </scm>
        <developers>
          <developer>
            <id>sgodbillon</id>
            <name>Stephane Godbillon</name>
            <url>http://stephane.godbillon.com</url>
          </developer>
          <developer>
            <id>cchantep</id>
            <name>Cedric Chantepie</name>
            <url>github.com/cchantep/</url>
          </developer>
        </developers>)
    )
  }

  @inline def env(n: String): String = sys.env.get(n).getOrElse(n)
}
