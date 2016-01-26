logLevel := Level.Warn

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
    Resolver.ivyStylePatterns)
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")
addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.0-RC6")