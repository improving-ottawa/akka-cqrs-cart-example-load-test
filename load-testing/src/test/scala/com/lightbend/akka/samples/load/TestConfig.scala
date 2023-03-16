package com.lightbend.akka.samples.load

import pureconfig._
import pureconfig.generic.semiauto._

import scala.concurrent.duration.FiniteDuration

case class TestConfig(randomPayloadSize: Int, targetHost: String, targetPort: Int, testDuration: FiniteDuration, usersPerSecond: Int)

object TestConfig {
  implicit val reader = deriveReader[TestConfig]
  def apply() = ConfigSource.resources("test.conf").at("shopping-cart-load-test").load[TestConfig].getOrElse(throw new IllegalStateException("Could not load test config"))
}
