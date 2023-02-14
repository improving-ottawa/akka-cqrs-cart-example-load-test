import Dependencies._

val gatling = "3.9.0"
val gatling_grpc = "0.15.1"

name := "shopping-cart-load-test"

organization := "com.lightbend.akka.samples"

lazy val loadTesting = project
  .in(file("."))
  .enablePlugins(GatlingPlugin)
  .settings(
    scalaVersion := "2.13.10",
    libraryDependencies ++= Seq(
    "io.gatling.highcharts" % "gatling-charts-highcharts" % gatling % Test,
    "io.gatling" % "gatling-test-framework" % gatling % Test,
    "com.github.phisgr" % "gatling-grpc" % "0.15.1" % Test),
    name := "load-testing",
    libraryDependencies ++= integrationTestDependencies
  )
