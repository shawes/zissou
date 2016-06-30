name := "zissou"

version := "1.0"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.1", "2.10.3")

scalacOptions ++= Seq("-deprecation", "-feature", "-language:implicitConversions")


resolvers += Resolver.sonatypeRepo("public")
assemblyJarName in assembly := "zissou.jar"


// geoTools
val geotoolsVersion = "13.2"

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "junit" % "junit" % "4.12" % "test",
  "org.mockito" % "mockito-core" % "2.0.40-beta",
  "com.github.nscala-time" %% "nscala-time" % "2.6.0",
  "org.apache.commons" % "commons-math3" % "3.6",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "org.slf4j" % "slf4j-api" % "1.7.14",
  "org.slf4j" % "slf4j-simple" % "1.7.14",
  "edu.ucar" % "cdm" % "4.6.0",
  "edu.ucar" % "netcdf" % "4.3.22",
  "org.geotools" % "gt-shapefile" % geotoolsVersion
)

// add scala-xml dependency when needed (for Scala 2.11 and newer) in a robust way
// this mechanism supports cross-version publishing
// taken from: http://github.com/scala/scala-module-dependency-sample
libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    // if scala 2.11+ is used, add dependency on scala-xml module
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3",
        "org.scala-lang.modules" %% "scala-swing" % "1.0.1")
    case _ =>
      // or just libraryDependencies.value if you don't depend on scala-swing
      libraryDependencies.value :+ "org.scala-lang" % "scala-swing" % scalaVersion.value
  }
}

// add scala-xml dependency when needed (for Scala 2.11 and newer)
// this mechanism supports cross-version publishing

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.3"
    case _ =>
      libraryDependencies.value
  }
}

resolvers ++= Seq(
  "OpenGeo Maven Repository" at "http://repo.opengeo.org",
  "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/",
  "maven Repository" at "http://artifacts.unidata.ucar.edu/content/repositories/unidata-releases/"
)