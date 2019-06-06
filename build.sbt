name := "zissou"

version := "2.1"

scalaVersion := "2.12.8"

assemblyJarName in assembly := "zissou.jar"

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case PathList("uom-se", xs @ _*) => MergeStrategy.discard
  case PathList("META-INF", xs @ _*) =>
    (xs map { _.toLowerCase }) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) |
          ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case _ => MergeStrategy.last
    }

  case x => MergeStrategy.first
}

//logLevel in assembly := Level.Debug

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:implicitConversions"
)

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.7",
  "org.scalatest" %% "scalatest" % "3.0.7" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.mockito" % "mockito-core" % "2.28.2" % "test",
  "com.github.nscala-time" %% "nscala-time" % "2.22.0",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "org.clapper" %% "grizzled-slf4j" % "1.3.1",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  //"org.slf4j" % "slf4j-jdk14" % "latest.integration",
  "edu.ucar" % "netcdf4" % "4.6.10",
  "edu.ucar" % "cdm" % "4.6.10",
  //"edu.ucar" % "opendap" % "latest.integration",
  //"org.scala-lang" % "scala-library" % "2.12.3",
  "org.geotools" % "gt-shapefile" % "21.1",
  "org.geotools" % "gt-main" % "21.1",
  "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar",
  "javax.media" % "jai_codec" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_codec/1.1.3/jai_codec-1.1.3.jar",
  "javax.media" % "jai_imageio" % "1.1" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_imageio/1.1/jai_imageio-1.1.jar",
  "com.luckycatlabs" % "SunriseSunsetCalculator" % "1.2",
  "org.scala-debugger" %% "scala-debugger-api" % "1.1.0-M3",
  "io.circe" %% "circe-yaml" % "0.10.0",
  "io.circe" %% "circe-generic" % "0.11.1",
  "io.circe" %% "circe-core" % "0.11.1"

  /*   "org.scalactic" %% "scalactic" % "3.0.7",
  "org.scalatest" %% "scalatest" % "3.0.7" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.mockito" % "mockito-core" % "2.28.2" % "test",
  "com.github.nscala-time" %% "nscala-time" % "2.22.0",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "org.clapper" %% "grizzled-slf4j" % "1.3.3",
  "org.slf4j" % "slf4j-api" % "1.7.26",
  "org.slf4j" % "slf4j-simple" % "1.7.26",
  "edu.ucar" % "cdm" % "4.6.13",
  "org.geotools" % "gt-shapefile" % "21.1",
  "org.geotools" % "gt-main" % "21.1",
  "org.geotools" % "gt-opengis" % "21.1",
  "org.geotools" % "gt-referencing" % "21.1",
  "org.geotools" % "gt-metadata" % "21.1",
  "org.locationtech.jts" % "jts-core" % "1.16.1",
  "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar",
  "javax.media" % "jai_codec" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_codec/1.1.3/jai_codec-1.1.3.jar",
  "javax.media" % "jai_imageio" % "1.1" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_imageio/1.1/jai_imageio-1.1.jar",
  "com.luckycatlabs" % "SunriseSunsetCalculator" % "1.2",
  //"org.scala-debugger" %% "scala-debugger-api" % "1.1.0-M3",
  "io.circe" %% "circe-yaml" % "0.10.0",
  "io.circe" %% "circe-generic" % "0.11.1",
  "io.circe" %% "circe-core" % "0.11.1" */
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("public"),
  "Locationtech-releases" at "https://repo.locationtech.org/content/groups/releases",
  "OpenGeo Maven Repository" at "http://repo.boundlessgeo.com/main/",
  "Unidata Releases" at "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases",
  "Artima Maven Repository" at "http://repo.artima.com/releases",
  "geosolutions" at "http://maven.geo-solutions.it/",
  "osgeo" at "http://download.osgeo.org/webdav/geotools/",
  "maven" at "http://central.maven.org/maven2/"
)
