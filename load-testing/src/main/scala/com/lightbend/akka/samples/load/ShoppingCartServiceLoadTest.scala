package com.lightbend.akka.samples.load

import ShoppingCartServiceLoadTest.Target
import com.github.phisgr.gatling.grpc.Predef._

import scala.concurrent.duration._
// stringToExpression is hidden because we have $ in GrpcDsl
import io.gatling.core.Predef.{stringToExpression => _, _}

class ShoppingCartServiceLoadTest extends Simulation {

  val localTarget = Target("127.0.0.1", 8101)
  val targetMap = Map(
    "local" -> localTarget,
    "dev" -> Target("127.0.0.1", 10110)
  )

  val target = targetMap.getOrElse(System.getProperty("targetEnvironment", "local"), localTarget)

  println(s"Using $target as target environment for load test")

  val randomPayloadSize = System.getProperty("randomPayloadSize", "50").toInt

  val grpcTarget = grpc(managedChannelBuilder(target.host, target.port).usePlaintext())

  val tests =
    List(
      ShoppingCartScenario(grpcTarget, randomPayloadSize)
    )

  private val users = System.getProperty("requestsPerSecond", "1").toInt  // # of users

  private val loadDuration: FiniteDuration = (1 * 60).seconds // test duration in seconds

  println(s"Starting Shopping cart load test at ${users} users per second")

  val testPlan = tests.flatMap(_.all.map(_.inject(constantUsersPerSec(users).during(loadDuration))))
  //val testPlan = tests.flatMap(_.all.map(_.inject(atOnceUsers(users))))
  setUp(
    testPlan
    )
    .protocols(grpcTarget)
    .assertions(
//      global.responseTime.mean.lt(100), // < 100ms response
//      global.successfulRequests.percent.gt(99)
    )
}

object ShoppingCartServiceLoadTest {
  case class Target(host: String, port: Int)
}
