logLevel := Level.Warn

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
// addSbtPlugin("org.scala-debugger" % "sbt-jdi-tools" % "1.0.0")
// addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
// addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")
// addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
//addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")
addSbtPlugin(
  "com.github.cb372" % "sbt-explicit-dependencies" % "0.2.9"
)
