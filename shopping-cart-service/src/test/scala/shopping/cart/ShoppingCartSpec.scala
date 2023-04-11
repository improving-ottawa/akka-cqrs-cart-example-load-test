package shopping.cart

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

object ShoppingCartSpec {
  val config = ConfigFactory
    .parseString("""
      akka.actor.serialization-bindings {
        "shopping.cart.CborSerializable" = jackson-cbor
      }
      """)
    .withFallback(EventSourcedBehaviorTestKit.config)
}

class ShoppingCartSpec
    extends ScalaTestWithActorTestKit(ShoppingCartSpec.config)
    with AnyWordSpecLike
    with BeforeAndAfterEach {

  private val cartId = "testCart"
  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[
      ShoppingCart.Command,
      ShoppingCart.Event,
      ShoppingCart.State](system, ShoppingCart(cartId, "carts-0"))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "The Shopping Cart" should {

    "add item" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          replyTo =>
            ShoppingCart.AddItem("foo", 42, new Array[Byte](0), replyTo))
      result1.reply should ===(
        StatusReply.Success(
          ShoppingCart.Summary(Map("foo" -> 42), checkedOut = false)))
      result1.event should ===(
        ShoppingCart.ItemAdded(cartId, "foo", 42, new Array[Byte](0)))
    }

    "reject already added item" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, new Array[Byte](0), _))
      result1.reply.isSuccess should ===(true)
      val result2 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 13, new Array[Byte](0), _))
      result2.reply.isError should ===(true)
    }

    "remove item" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, new Array[Byte](0), _))
      result1.reply.isSuccess should ===(true)
      val result2 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.RemoveItem("foo", _))
      result2.reply should ===(
        StatusReply.Success(
          ShoppingCart.Summary(Map.empty, checkedOut = false)))
      result2.event should ===(ShoppingCart.ItemRemoved(cartId, "foo", 42))
    }

    "adjust quantity" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, new Array[Byte](0), _))
      result1.reply.isSuccess should ===(true)
      val result2 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AdjustItemQuantity("foo", 43, new Array[Byte](0), _))
      result2.reply should ===(
        StatusReply.Success(
          ShoppingCart.Summary(Map("foo" -> 43), checkedOut = false)))
      result2.event should ===(
        ShoppingCart
          .ItemQuantityAdjusted(cartId, "foo", 43, 42, new Array[Byte](0)))
    }

    "checkout" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, new Array[Byte](0), _))
      result1.reply.isSuccess should ===(true)
      val result2 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.Checkout(new Array[Byte](0), _))
      result2.reply should ===(
        StatusReply.Success(
          ShoppingCart.Summary(Map("foo" -> 42), checkedOut = true)))
      result2.event.asInstanceOf[ShoppingCart.CheckedOut].cartId should ===(
        cartId)

      val result3 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("bar", 13, new Array[Byte](0), _))
      result3.reply.isError should ===(true)
    }

    "get" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, new Array[Byte](0), _))
      result1.reply.isSuccess should ===(true)

      val result2 = eventSourcedTestKit.runCommand[ShoppingCart.Summary](
        ShoppingCart.Get(_))
      result2.reply should ===(
        ShoppingCart.Summary(Map("foo" -> 42), checkedOut = false))
    }

    "keep its state" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, new Array[Byte](0), _))
      result1.reply should ===(
        StatusReply.Success(
          ShoppingCart.Summary(Map("foo" -> 42), checkedOut = false)))

      eventSourcedTestKit.restart()

      val result2 = eventSourcedTestKit.runCommand[ShoppingCart.Summary](
        ShoppingCart.Get(_))
      result2.reply should ===(
        ShoppingCart.Summary(Map("foo" -> 42), checkedOut = false))
    }
  }

}
