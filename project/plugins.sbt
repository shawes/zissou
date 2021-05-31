logLevel := Level.Warn

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"
// addSbtPlugin("org.scala-debugger" % "sbt-scala-debugger" % "1.1.0-M3")
// addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
// addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")
// addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
//addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")
addSbtPlugin(
  "com.github.cb372" % "sbt-explicit-dependencies" % "0.2.13"
)
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.1")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.28")
addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.4.2")
