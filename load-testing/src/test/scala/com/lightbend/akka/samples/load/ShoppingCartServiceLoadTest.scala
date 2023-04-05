package com.lightbend.akka.samples.load

import ShoppingCartServiceLoadTest.Target
import com.github.phisgr.gatling.grpc.Predef._

import scala.concurrent.duration._
// stringToExpression is hidden because we have $ in GrpcDsl
import io.gatling.core.Predef.{stringToExpression => _, _}

class ShoppingCartServiceLoadTest extends Simulation {

  val testConfig = TestConfig()

  println(s"Using ${testConfig.targetHost}:${testConfig.targetPort} as target environment for load test")

  val randomPayloadSize = testConfig.randomPayloadSize

  val grpcTarget = grpc(managedChannelBuilder(testConfig.targetHost, testConfig.targetPort).usePlaintext())

  val tests =
    List(
      ShoppingCartScenario(grpcTarget, randomPayloadSize)
    )

  private val users =  testConfig.usersPerSecond

  private val rampFrom = testConfig.rampFrom
  private val rampTo = testConfig.rampTo


  println(s"Starting Shopping cart load test at ${users} users per second")

  val testPlan = tests.flatMap(_.all.map(_.inject(
    constantUsersPerSec(users).during(loadDuration),
    rampUsersPerSec(rampFrom).to(rampTo).during(testConfig.rampOver)
  )))
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
