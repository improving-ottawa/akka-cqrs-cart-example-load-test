import Dependencies._

val gatling = "3.9.2"
val gatling_grpc = "0.15.1"

name := "shopping-cart-load-test"

organization := "com.lightbend.akka.samples"

lazy val loadTesting = project
  .in(file("."))
  .enablePlugins(GatlingPlugin)
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    scalaVersion := "2.13.10",
    libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files" % "3.9.2",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.1",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.14.1",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.14.1",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.1",
      "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % "2.14.1",
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.14.1",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.14.1",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.14.1",
      "io.gatling.highcharts" % "gatling-charts-highcharts" % gatling,
      "io.gatling" % "gatling-test-framework" % gatling,
      "com.github.phisgr" % "gatling-grpc" % "0.15.1"
    ),
    dependencyOverrides ++= Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"
    ),
    name := "shopping-cart-load-test-driver",
    libraryDependencies ++= integrationTestDependencies,
    dockerBaseImage := "docker.io/library/adoptopenjdk:11-jre-hotspot",
    dockerUsername := sys.props.get("docker.username"),
    dockerRepository := sys.props.get("docker.registry"),
    dockerRepository := Some("shopping-cart-load-test"),
    Docker / packageName := "shopping-cart-load-test-driver"
  )
