name := "zissou"

version := "1.0"

crossScalaVersions := Seq("2.11.8", "2.11.1", "2.10.3")

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-deprecation", "-feature", "-language:implicitConversions")


resolvers += Resolver.sonatypeRepo("public")
assemblyJarName in assembly := "zissou.jar"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.mockito" % "mockito-core" % "2.7.17",
  "com.github.nscala-time" %% "nscala-time" % "2.16.0",
  "org.apache.commons" % "commons-math3" % "3.6",
  "org.clapper" %% "grizzled-slf4j" % "1.3.0",
  "org.slf4j" % "slf4j-api" % "latest.integration",
  "org.slf4j" % "slf4j-simple" % "latest.integration",
  //"org.slf4j" % "slf4j-jdk14" % "latest.integration",
  "edu.ucar" % "netcdf4" % "4.6.8",
  "edu.ucar" % "cdm" % "4.6.8",
  //"edu.ucar" % "opendap" % "latest.integration",
  "org.geotools" % "gt-shapefile" % "17-RC1"
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
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
        "org.scala-lang.modules" %% "scala-swing" % "2.0.0-M2")
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
  "OpenGeo Maven Repository" at "http://repo.opengeo.org",
  "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/",
  "Unidata Releases" at "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases/",
  "Artima Maven Repository" at "http://repo.artima.com/releases"
)
