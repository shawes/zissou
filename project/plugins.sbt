logLevel := Level.Warn

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"
addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.2")
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
