name := "zissou"

version := "1.0"

scalaVersion := "2.12.8"

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
  "org.scalactic" %% "scalactic" % "3.0.6",
  "org.scalatest" %% "scalatest" % "3.0.6" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.mockito" % "mockito-core" % "2.24.5" % "test",
  "com.github.nscala-time" %% "nscala-time" % "2.22.0",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "org.clapper" %% "grizzled-slf4j" % "1.3.3",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "edu.ucar" % "netcdf4" % "4.6.13",
  "edu.ucar" % "cdm" % "4.6.13",
  "org.geotools" % "gt-shapefile" % "21.0",
  "org.geotools" % "gt-main" % "21.0",
  "org.locationtech.jts" %"jts-core" % "1.16.1",
  "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar",
  "javax.media" % "jai_codec" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_codec/1.1.3/jai_codec-1.1.3.jar",
  "javax.media" % "jai_imageio" % "1.1" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_imageio/1.1/jai_imageio-1.1.jar",
  "com.luckycatlabs" % "SunriseSunsetCalculator" % "1.2",
  "org.scala-debugger" %% "scala-debugger-api" % "1.1.0-M3",
  "io.circe" %% "circe-core" % "0.10.0",
  "io.circe" %% "circe-generic"% "0.10.0",
  "io.circe" %% "circe-parser" % "0.10.0",
  "io.circe" %% "circe-yaml" % "0.8.0"
)


// add scala-xml dependency when needed (for Scala 2.11 and newer) in a robust way
// this mechanism supports cross-version publishing
// taken from: http://github.com/scala/scala-module-dependency-sample
libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    // if Scala 2.12+ is used, use scala-swing 2.x
    case Some((2, scalaMajor)) if scalaMajor >= 12 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.1.1",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1",
        "org.scala-lang.modules" %% "scala-swing" % "2.1.0")
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
  "Locationtech-releases" at "https://repo.locationtech.org/content/groups/releases",
  "OpenGeo Maven Repository" at "http://repo.boundlessgeo.com/main/",
  "Unidata Releases" at "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases/",
  "Artima Maven Repository" at "http://repo.artima.com/releases"
)
