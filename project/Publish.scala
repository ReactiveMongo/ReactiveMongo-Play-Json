import sbt.Keys._
import sbt._

object Publish {
  lazy val settings = {
    val repoName = env("PUBLISH_REPO_NAME")
    val repoUrl = env("PUBLISH_REPO_URL")

    Seq(
      publishMavenStyle := true,
      Test / publishArtifact := false,
      publishTo := Some(repoUrl).map(repoName at _),
      credentials += Credentials(repoName, env("PUBLISH_REPO_ID"),
        env("PUBLISH_USER"), env("PUBLISH_PASS")),
      pomIncludeRepository := { _ => false },
      licenses := {
        Seq("Apache 2.0" ->
          url("http://www.apache.org/licenses/LICENSE-2.0"))
      },
      homepage := Some(url("http://reactivemongo.org")),
      autoAPIMappings := true,
      pomExtra := (
        <scm>
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
        </developers>))
  }

  @inline def env(n: String): String = sys.env.get(n).getOrElse(n)
}
