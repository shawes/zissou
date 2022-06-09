ThisBuild / scalaVersion := "3.1.2"
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
      "edu.ucar" % "netcdfAll" % "5.5.2",
      "org.geotools" % "gt-shapefile" % "27.0",
      "org.geotools" % "gt-main" % "27.0",
      "com.luckycatlabs" % "SunriseSunsetCalculator" % "1.2",
      "org.scalatestplus" %% "mockito-4-5" % "3.2.12.0" % "test",
      "org.scalatestplus" %% "scalacheck-1-16" % "3.2.12.0" % "test",
      "org.scalactic" %% "scalactic" % "3.2.12",
      "org.scalatest" %% "scalatest" % "3.2.12" % "test",
      "io.circe" %% "circe-core" % "0.14.2",
      "io.circe" %% "circe-generic" % "0.14.2",
      "com.github.nscala-time" %% "nscala-time" % "2.28.0",
      "io.circe" %% "circe-yaml" % "0.14.0"
    ),
    libraryDependencies += ("org.clapper" %% "grizzled-slf4j" % "1.3.4")
      .cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4")
      .cross(CrossVersion.for3Use2_13),
    maxErrors := 5
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
          "-deprecation", // emit warning and location for usages of deprecated APIs
          "-explain", // explain errors in more detail
          "-explain-types", // explain type errors in more detail
          "-feature", // emit warning and location for usages of features that should be imported explicitly
          "-indent", // allow significant indentation.
          "-new-syntax", // require `then` and `do` in control expressions.
          "-print-lines", // show source code line numbers.
          "-unchecked", // enable additional warnings where generated code depends on assumptions
          "-Ykind-projector", // allow `*` as wildcard to be compatible with kind projector
          "-Xfatal-warnings", // fail the compilation if there are any warnings
          "-Xmigration" // warn about constructs whose behavior may have changed since version
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

Test / parallelExecution := false

resolvers ++= Seq(
  Resolver.sonatypeRepo("public"),
  "osgeo" at "https://repo.osgeo.org/repository/release/"
)
