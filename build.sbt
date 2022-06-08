ThisBuild / scalaVersion := "3.1.1"
ThisBuild / organization := "hawes.zissou"
ThisBuild / version := "3.0"

crossScalaVersions ++= Seq("2.13.8", "3.0.0")

lazy val zissou = (project in file("."))
  .settings(
    name := "zissou",
    assembly / mainClass := Some("model.Simulator"),
    assembly / assemblyJarName := "zissou.jar",
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.13.2" % "test",
      "org.apache.commons" % "commons-math3" % "3.6.1",
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "org.slf4j" % "slf4j-simple" % "1.7.30",
      "edu.ucar" % "netcdfAll" % "5.4.1",
      "org.geotools" % "gt-shapefile" % "25.0",
      "org.geotools" % "gt-main" % "25.0",
      "com.luckycatlabs" % "SunriseSunsetCalculator" % "1.2",
      "org.scalatestplus" %% "mockito-4-5" % "3.2.12.0" % "test",
      "org.scalatestplus" %% "scalacheck-1-16" % "3.2.12.0" % "test",
      "org.scalatest" %% "scalatest" % "3.2.12" % "test",
      "io.circe" %% "circe-core" % "0.14.0-M7",
      "io.circe" %% "circe-generic" % "0.14.1",
      "com.github.nscala-time" %% "nscala-time" % "2.28.0",
      "io.circe" %% "circe-yaml" % "0.14.0"
    ),
    libraryDependencies += ("org.clapper" %% "grizzled-slf4j" % "1.3.4")
      .cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4")
      .cross(CrossVersion.for3Use2_13)
  )

ThisBuild / assemblyMergeStrategy := {
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

//logLevel / assembly := Level.Debug

scalacOptions ++= {
  Seq(
    "-encoding",
    "UTF-8"
    // "-feature",
    // "-language:implicitConversions"
    // disabled during the migration
    // "-Xfatal-warnings"
  ) ++
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        Seq(
          "-unchecked",
          "-source:3.0-migration"
        )
      case _ =>
        Seq(
          "-deprecation",
          "-Xfatal-warnings",
          "-Wunused:imports,privates,locals",
          "-Wvalue-discard"
        )
    })
}

scalacOptions ++= Seq("-Xmax-inlines", "50")

Test / parallelExecution := false

resolvers ++= Seq(
  Resolver.sonatypeRepo("public"),
  "osgeo" at "https://repo.osgeo.org/repository/release/"
)
