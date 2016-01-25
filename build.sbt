name := "zissou"

version := "1.0"

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.11.1", "2.10.3")

scalacOptions ++= Seq("-deprecation", "-feature", "-language:implicitConversions")

Seq(bintrayResolverSettings: _*)



// test
libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.1.7" % "test",
  "junit" % "junit" % "4.10" % "test",
  "org.mockito" % "mockito-core" % "1.9.5"
)



// geoTools
val geotoolsVersion = "12-RC1"

libraryDependencies ++= Seq(
  "org.geotools" % "gt-shapefile" % geotoolsVersion,
  "org.geotools" % "gt-epsg-hsql" % geotoolsVersion,
  "org.geotools" % "gt-swing" % geotoolsVersion
)

//other
libraryDependencies ++= Seq(
  "com.github.nscala-time" % "nscala-time_2.11" % "1.2.0",
  "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.2",
  "org.scala-lang.modules" % "scala-swing_2.11" % "1.0.1",
  "org.apache.commons" % "commons-math" % "2.2"
)

//logging
libraryDependencies ++= Seq(
  //"ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.clapper" %% "grizzled-scala" % "1.2",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "org.clapper" %% "avsl" % "1.0.2"
)


// add scala-xml dependency when needed (for Scala 2.11 and newer)
// this mechanism supports cross-version publishing

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
    case _ =>
      libraryDependencies.value
  }
}

resolvers ++= Seq(
  "OpenGeo Maven Repository" at "http://repo.opengeo.org",
  "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/"
)