package load

import GoGoGatling.TestVariables
import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}

import scala.concurrent.duration._

trait GoGoGatling {
  val vars: TestVariables = TestVariables(1.seconds, 2000)

//  val pause: ChainBuilder =
//    exec { session =>
//      Thread.sleep(vars.eventualConsistencyMs)
//      session
//    }

  def all: List[ScenarioBuilder]
}

object GoGoGatling {
  case class TestVariables(pauseTime: FiniteDuration, eventualConsistencyMs: Long)
}
