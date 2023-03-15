package com.lightbend.akka.samples.load

import com.github.phisgr.gatling.grpc.Predef._
import com.github.phisgr.gatling.grpc.protocol.GrpcProtocol
import com.lightbend.akka.samples.load.ShoppingCartScenario.Catalogue
import io.gatling.commons.validation.Success
import shopping.cart.proto.{AddItemRequest, CheckoutRequest, ShoppingCartServiceGrpc, UpdateItemRequest}
// stringToExpression is hidden because we have $ in GrpcDsl
import io.gatling.core.Predef.{stringToExpression => _, _}
import io.gatling.core.feeder.Feeder
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioBuilder
import io.grpc.Status
import org.scalacheck.Gen

/**
 * Simple scenario that exercises write side of the shopping cart service.
 *
 * - Assigns a cart ID
 * - selects a portion of the catalogue to add to the cart
 * - adds an item and then adjusts it by a random number of times, repeats until the selected portion of the catalogue has been added
 * - checks out
 */
class ShoppingCartScenario(catalogue: Catalogue, randomPayloadBytes: Int)(protocol: GrpcProtocol) extends GoGoGatling {

  private val quantityGen = Gen.choose(3, 7000)
  private val numberOfUpdateGen = Gen.choose(5, 10)
  private val randomPayloadGen = Gen.listOfN(randomPayloadBytes, Gen.hexChar).map(_.map(_.toByte).toArray)

  private val scenarioInputGen: Gen[Map[String, Any]] = {
    for {
      itemId <- catalogue.randomItemGen
      firstQuantity <- quantityGen
      numberOfUpdates <- numberOfUpdateGen
    } yield {
      Map(
        "itemId" -> itemId,
        "firstQuantity" -> firstQuantity,
        "numberOfUpdates" -> numberOfUpdates
      )
    }
  }

  private val fillCartFeeder: Feeder[Any] = Iterator.continually(scenarioInputGen.sample.get)

  private val addItemRequestExpr: Expression[AddItemRequest] = { s =>
    for {
      cartId <- s("cartId").validate[String]
      itemId <- s("itemId").validate[String]
    } yield AddItemRequest(cartId, itemId, quantityGen.sample.get, com.google.protobuf.ByteString.copyFrom(randomPayloadGen.sample.get))
  }

  private val updateItemRequestExpr: Expression[UpdateItemRequest] = { s =>
    for {
      cartId <- s("cartId").validate[String]
      itemId <- s("itemId").validate[String]
    } yield UpdateItemRequest(cartId, itemId, quantityGen.sample.get, com.google.protobuf.ByteString.copyFrom(randomPayloadGen.sample.get))
  }

  val test: ScenarioBuilder =
    scenario("Fill a shopping cart and check out")
      .exec(_.set("cartId", ShoppingCartScenario.shoppingCartIdGen.sample.get))
      .repeat(_ => Success(catalogue.randomPortionOfItems().sample.get)) {
        feed(fillCartFeeder)
          .exec(
            grpc(_ => "Add Cart Item")
              .rpc(ShoppingCartServiceGrpc.METHOD_ADD_ITEM)
              .payload(addItemRequestExpr)
              .check(statusCode is Status.Code.OK)
              // TODO:  expand this checker to account for case where we've already added
              .target(protocol)
          )
          .repeat(_("numberOfUpdates").validate[Int]) {
            exec(
              grpc(_ => "Update Cart Item")
                .rpc(ShoppingCartServiceGrpc.METHOD_UPDATE_ITEM)
                .payload(updateItemRequestExpr)
                .check(statusCode is Status.Code.OK)
                .target(protocol)
            )
          }
      }
      .exec(
        grpc(_ => "Checkout")
          .rpc(ShoppingCartServiceGrpc.METHOD_CHECKOUT)
          .payload(s => s("cartId").validate[String].map(CheckoutRequest(_)))
          .check(statusCode is Status.Code.OK)
          .target(protocol)
      )

  def all: List[ScenarioBuilder] = List(test)
}

object ShoppingCartScenario {

  def apply(protocol: GrpcProtocol, payloadBytes: Int = 50): ShoppingCartScenario =
    new ShoppingCartScenario(catalogueGen.sample.get, payloadBytes)(protocol)

  val catalogueSizeGen = Gen.choose(50, 200)

  /**
   * Holds a catalogue of available items
   */
  case class Catalogue(items: Map[String, Boolean]) {
    private val randomI = Gen.choose(0, items.size - 1)
    private val itemsList = items.toList

    def randomItemGen: Gen[String] = randomI.map(itemsList(_)._1)
    def randomPortionOfItems(min: Double = .5, max: Double = .8) = Gen.choose(min,max).map(_ * itemsList.size).map(_.toInt)
  }

  val catalogueGen = Gen.listOfN(catalogueSizeGen.sample.get, Gen.stringOfN(25, Gen.alphaChar)).map { items =>
    Catalogue(items.map(_ -> false).toMap)
  }

  val shoppingCartIdGen = Gen.uuid.map(_.toString)
}
