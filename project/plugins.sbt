resolvers ++= Seq(
  Resolver.bintrayIvyRepo("typesafe", "sbt-plugins"),
  "Tatami Releases" at "https://raw.github.com/cchantep/tatami/master/releases"
)

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")

addSbtPlugin("cchantep" % "sbt-scaladoc-compiler" % "0.3")

addSbtPlugin("cchantep" % "sbt-hl-compiler" % "0.8")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.4")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.1.0")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.4.0")

addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.0.1")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.12.1")

addDependencyTreePlugin
