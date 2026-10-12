name := "zissou"

version := "2.1"

scalaVersion := "3.10.0"

enablePlugins(JavaAppPackaging)

// `sbt stage` lays the program out as target/universal/stage/lib/*.jar, which the
// scripts in scripts/ run directly; there is no fat jar
Compile / mainClass := Some("model.Simulator")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:implicitConversions"
)

Test / parallelExecution := false

// the web interface starts the simulator in a second JVM, which needs the project's
// classpath; a forked test JVM has it, sbt's own does not
Test / fork := true

val geotoolsVersion = "35.1"
val netcdfVersion = "5.11.0"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.20" % Test,
  "junit" % "junit" % "4.13.2" % Test,
  "org.scalatestplus" %% "mockito-5-23" % "3.2.20.0" % Test,
  "com.github.nscala-time" %% "nscala-time" % "3.2.0",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  // no Scala 3 build exists; it is a thin pure-Scala wrapper over slf4j, so the 2.13 one works
  ("org.clapper" %% "grizzled-slf4j" % "1.3.4").cross(CrossVersion.for3Use2_13),
  "org.slf4j" % "slf4j-api" % "2.0.17",
  "org.slf4j" % "slf4j-simple" % "2.0.17",
  "org.scala-lang.modules" %% "scala-xml" % "2.5.0",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0",
  "edu.ucar" % "cdm-core" % netcdfVersion,
  "org.geotools" % "gt-shapefile" % geotoolsVersion,
  "org.geotools" % "gt-main" % geotoolsVersion,
  "org.geotools" % "gt-referencing" % geotoolsVersion,
  "com.luckycatlabs" % "SunriseSunsetCalculator" % "1.2",
  "io.circe" %% "circe-yaml" % "0.16.1",
  "io.circe" %% "circe-core" % "0.14.17"
)

resolvers ++= Seq(
  "OSGeo" at "https://repo.osgeo.org/repository/release/",
  "Unidata" at "https://artifacts.unidata.ucar.edu/repository/unidata-releases/"
)
