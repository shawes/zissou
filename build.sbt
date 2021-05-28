name := "zissou"

version := "3.0"

scalaVersion := "2.13.3"

assemblyJarName in assembly := "zissou.jar"

test in assembly := {}

mainClass in assembly := Some("model.Simulator")

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
  "org.scalactic" %% "scalactic" % "3.2.0",
  "org.scalatest" %% "scalatest" % "3.2.0" % "test",
  "org.scalatestplus" %% "scalacheck-1-14" % "3.2.1.0" % "test",
  "junit" % "junit" % "4.13" % "test",
  "org.mockito" % "mockito-core" % "3.4.0" % "test",
  "com.github.nscala-time" %% "nscala-time" % "2.24.0",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "org.clapper" %% "grizzled-slf4j" % "1.3.4",
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "org.slf4j" % "slf4j-simple" % "1.7.30",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0",
  //"org.slf4j" % "slf4j-jdk14" % "latest.integration",
  "edu.ucar" % "netcdfAll" % "5.3.3",
  //"edu.ucar" % "opendap" % "latest.integration",
  //"org.scala-lang" % "scala-library" % "2.12.3",
  "org.geotools" % "gt-shapefile" % "23.0",
  "org.geotools" % "gt-main" % "23.0",
  "javax.media" % "jai_core" % "1.1.3",
  "javax.media" % "jai_codec" % "1.1.3" % Test,
  "javax.media" % "jai_imageio" % "1.1.1",
  "com.luckycatlabs" % "SunriseSunsetCalculator" % "1.2",
  //"org.scala-debugger" %% "scala-debugger-api" % "1.1.0-M3",
  "io.circe" %% "circe-yaml" % "0.13.1",
  "io.circe" %% "circe-generic" % "0.13.0",
  "io.circe" %% "circe-core" % "0.13.0"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest-flatspec" % "3.2.0" % "test",
  "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.0" % "test",
  "org.scalatestplus" %% "mockito-3-3" % "3.2.0.0" % "test"
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("public"),
  //"Locationtech-releases" at "https://repo.locationtech.org/content/groups/releases",
  //"OpenGeo Maven Repository" at "http://repo.boundlessgeo.com/main/",
  //"Unidata Releases" at "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases",
  "Artima Maven Repository" at "https://repo.artima.com/releases",
  //"geosolutions" at "http://maven.geo-solutions.it/",
  "osgeo" at "https://repo.osgeo.org/repository/release/",
  "Geotoolkit" at "https://maven.geotoolkit.org/"
  //"maven" at "http://central.maven.org/maven2/"
)
