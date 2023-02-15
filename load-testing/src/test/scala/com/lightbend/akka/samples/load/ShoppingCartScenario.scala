package com.lightbend.akka.samples.load

import com.github.phisgr.gatling.grpc.Predef._
import com.github.phisgr.gatling.grpc.protocol.GrpcProtocol
import com.github.phisgr.gatling.pb._
import com.lightbend.akka.samples.load.ShoppingCartScenario.Catalogue
import shopping.cart.proto.{AddItemRequest, ShoppingCartServiceGrpc}
// stringToExpression is hidden because we have $ in GrpcDsl
import io.gatling.core.Predef.{stringToExpression => _, _}
import io.gatling.core.feeder.Feeder
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioBuilder
import io.grpc.Status
import org.scalacheck.Gen

class ShoppingCartScenario(cartId: String, catalogue: Catalogue)(protocol: GrpcProtocol) extends GoGoGatling {

  var catalogueInUse = catalogue

  val quantityGen = Gen.choose(3, 7000)

  val addItemQueryFeeder: Feeder[Any] = Iterator.continually(Map(
    "itemId" -> ShoppingCartScenario.shoppingCartIdGen.sample.get,
    "cartId" -> cartId,
    "quantity" -> quantityGen.sample.get
  ))

  val addItemRequestExpr: Expression[AddItemRequest] = AddItemRequest().updateExpr(
    _.itemId :~ $("itemId"),
    _.cartId :~ $("cartId"),
    _.quantity :~ $("quantity")
  )

  val test: ScenarioBuilder =
    scenario("Convert amount")
      .feed(addItemQueryFeeder)
      .exec(
        grpc(_ => "Add Item")
          .rpc(ShoppingCartServiceGrpc.METHOD_ADD_ITEM)
          .payload(addItemRequestExpr)
//          .check(statusCode is Status.Code.OK)
          .target(protocol)
      )
      .pause(vars.pauseTime)


  def all: List[ScenarioBuilder] = List(test)
}

object ShoppingCartScenario {

  def apply(protocol: GrpcProtocol): ShoppingCartScenario =
    new ShoppingCartScenario(shoppingCartIdGen.sample.get, catalogueGen.sample.get)(protocol)

  val catalogueSizeGen = Gen.choose(1000, 5000)

  case class Catalogue(items: Map[String, Boolean]) {
    private val randomI = Gen.choose(0, items.size)

    def randomItem = items.toList(randomI.sample.get)._1

    def useItem(item: String): Catalogue = copy(items + (item -> true))
  }

  val catalogueGen = Gen.listOfN(catalogueSizeGen.sample.get, Gen.asciiStr).map { items =>
    Catalogue(items.map(_ -> false).toMap)
  }

  val shoppingCartIdGen = Gen.identifier
}
