name := "zissou"

version := "3.0"

scalaVersion := "3.0.0"
crossScalaVersions ++= Seq("2.13.6", "3.0.0")

assembly / assemblyJarName := "zissou.jar"

assembly / test := {}

assembly / mainClass := Some("model.Simulator")

assembly / assemblyMergeStrategy := {
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

logLevel in assembly := Level.Debug

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:implicitConversions",
  "-source:3.0-migration"
)

scalacOptions ++= Seq("-Xmax-inlines", "50")

Test / parallelExecution := false

libraryDependencies ++= Seq(
  //"org.scalatest" %% "scalatest" % "3.2.8",
  "junit" % "junit" % "4.13.2" % "test",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  //"org.slf4j" % "slf4j-jdk14" % "latest.integration",
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "org.slf4j" % "slf4j-simple" % "1.7.30",
  "edu.ucar" % "netcdfAll" % "5.4.1",
  //"edu.ucar" % "opendap" % "latest.integration",
  //"org.scala-lang" % "scala-library" % "2.12.3",
  "org.geotools" % "gt-shapefile" % "25.0",
  "org.geotools" % "gt-main" % "25.0",
  //"javax.media" % "jai_core" % "1.1.3",
  //"javax.media" % "jai_codec" % "1.1.3" % Test,
  //"javax.media" % "jai_imageio" % "1.1.1",
  "com.luckycatlabs" % "SunriseSunsetCalculator" % "1.2",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.3",
  "org.scalatestplus" %% "mockito-3-4" % "3.2.9.0" % "test",
  "org.scalactic" %% "scalactic" % "3.2.9" % "test",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test",
  "io.circe" %% "circe-core" % "0.14.0-M7",
  "io.circe" %% "circe-generic" % "0.14.1",
  "com.github.nscala-time" %% "nscala-time" % "2.28.0"

  //"org.scala-debugger" %% "scala-debugger-api" % "1.1.0-M3",
)

libraryDependencies += ("org.mockito" %% "mockito-scala" % "1.16.37")
  .cross(CrossVersion.for3Use2_13)
libraryDependencies += ("org.clapper" %% "grizzled-slf4j" % "1.3.4")
  .cross(CrossVersion.for3Use2_13)
libraryDependencies += ("io.circe" %% "circe-yaml" % "0.13.1")
  .cross(CrossVersion.for3Use2_13)

resolvers ++= Seq(
  Resolver.sonatypeRepo("public"),
  "Artima Maven Repository" at "https://repo.artima.com/releases",
  "osgeo" at "https://repo.osgeo.org/repository/release/"
)
