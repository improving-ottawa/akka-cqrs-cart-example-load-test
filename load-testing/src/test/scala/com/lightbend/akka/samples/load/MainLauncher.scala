package com.lightbend.akka.samples.load

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object MainLauncher extends App {
  val props = new GatlingPropertiesBuilder()
  props.simulationClass(ShoppingCartServiceLoadTest.getClass.getName)
  Gatling.fromMap(props.build)
}
