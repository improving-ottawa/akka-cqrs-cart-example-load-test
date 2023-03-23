package com.lightbend.akka.samples.load

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

import java.time.Instant
import better.files._

object MainLauncher extends App {
  val resultsDirBase = sys.env.getOrElse("RESULT_BASE_DIR", "/")
  val pod = sys.env.getOrElse("POD_NAME", Instant.now().toEpochMilli.toString)
  val resultsPath = s"$resultsDirBase" + s"results-$pod"
  resultsPath.toFile.createDirectoryIfNotExists()
  val props = new GatlingPropertiesBuilder()
    .simulationClass(new ShoppingCartServiceLoadTest().getClass.getName)
    .noReports()
    .resultsDirectory(resultsPath)
  Gatling.fromMap(props.build)
}
