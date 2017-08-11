logLevel := Level.Warn

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.2")
// addSbtPlugin("org.scala-debugger" % "sbt-jdi-tools" % "1.0.0")
// addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
// addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")
// addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
//addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")
