name := "zissou"

version := "2.1"

scalaVersion := "2.12.8"

assemblyJarName in assembly := "zissou.jar"

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

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
  "org.mockito" % "mockito-core" % "2.27.0" % "test",
  "com.github.nscala-time" %% "nscala-time" % "2.22.0",
  "org.apache.commons" % "commons-math3" % "3.6.1" % "provided",
  "org.clapper" %% "grizzled-slf4j" % "1.3.3",
  "org.slf4j" % "slf4j-api" % "1.7.26",
  "org.slf4j" % "slf4j-simple" % "1.7.26",
  "edu.ucar" % "cdm" % "4.6.13",
  "javax.media" % "jai_core" % "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar",
  "org.geotools" % "gt-shapefile" % "21.0",
  "org.geotools" % "gt-main" % "21.0",
  "org.geotools" % "gt-opengis" % "21.0",
  "org.geotools" % "gt-referencing" % "21.0",
  "org.locationtech.jts" % "jts-core" % "1.16.1",
  "com.luckycatlabs" % "SunriseSunsetCalculator" % "1.2",
  //"org.scala-debugger" %% "scala-debugger-api" % "1.1.0-M3",
  "io.circe" %% "circe-yaml" % "0.10.0",
  "io.circe" %% "circe-generic" % "0.11.1",
  "io.circe" %% "circe-core" % "0.11.1",
  "org.typelevel" %% "cats-core" % "1.6.0",
  "joda-time" % "joda-time" % "2.10.2",
  "com.chuusai" %% "shapeless" % "2.3.3"
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("public"),
  "Locationtech-releases" at "https://repo.locationtech.org/content/groups/releases",
  "OpenGeo Maven Repository" at "http://repo.boundlessgeo.com/main/",
  "Unidata Releases" at "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases/",
  "Artima Maven Repository" at "http://repo.artima.com/releases",
  "geosolutions" at "http://maven.geo-solutions.it/",
  "osgeo" at "http://download.osgeo.org/webdav/geotools/",
  "maven" at "http://central.maven.org/maven2/"
)
