name := "zissou"

version := "1.0"

scalaVersion := "2.12.4"

scalacOptions ++= Seq("-deprecation", "-feature", "-language:implicitConversions")

parallelExecution in Test := false

resolvers += Resolver.sonatypeRepo("public")

assemblyJarName in assembly := "zissou.jar"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "jdom-info.xml") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.4",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.mockito" % "mockito-core" % "2.8.47" % "test",
  "com.github.nscala-time" %% "nscala-time" % "2.18.0",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "org.clapper" %% "grizzled-slf4j" % "1.3.1",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  //"org.slf4j" % "slf4j-jdk14" % "latest.integration",
  "edu.ucar" % "netcdf4" % "4.6.10",
  "edu.ucar" % "cdm" % "4.6.10",
  //"edu.ucar" % "opendap" % "latest.integration",
  //"org.scala-lang" % "scala-library" % "2.12.3",
  "org.geotools" % "gt-shapefile" % "17.2",
  "org.geotools" % "gt-main" % "17.2",
  "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar",
  "javax.media" % "jai_codec" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_codec/1.1.3/jai_codec-1.1.3.jar",
  "javax.media" % "jai_imageio" % "1.1" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_imageio/1.1/jai_imageio-1.1.jar",
  "com.luckycatlabs" % "SunriseSunsetCalculator" % "1.2",
  "org.scala-debugger" %% "scala-debugger-api" % "1.1.0-M3"
)


// add scala-xml dependency when needed (for Scala 2.11 and newer) in a robust way
// this mechanism supports cross-version publishing
// taken from: http://github.com/scala/scala-module-dependency-sample
libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    // if Scala 2.12+ is used, use scala-swing 2.x
    case Some((2, scalaMajor)) if scalaMajor >= 12 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
        "org.scala-lang.modules" %% "scala-swing" % "2.0.0")
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
        "org.scala-lang.modules" %% "scala-swing" % "1.0.2")
    case _ =>
      // or just libraryDependencies.value if you don't depend on scala-swing
      libraryDependencies.value :+ "org.scala-lang" % "scala-swing" % scalaVersion.value
  }
}

resolvers ++= Seq(
  "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/",
  "OpenGeo Maven Repository" at "http://repo.opengeo.org",
  "Unidata Releases" at "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases/",
  "Artima Maven Repository" at "http://repo.artima.com/releases"
)
