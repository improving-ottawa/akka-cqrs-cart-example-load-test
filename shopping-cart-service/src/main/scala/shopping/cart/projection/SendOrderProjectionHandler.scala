package shopping.cart.projection

import akka.Done
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import akka.util.Timeout
import org.slf4j.LoggerFactory
import shopping.cart.ShoppingCart
import shopping.order.proto.{ Item, OrderRequest, ShoppingOrderService }

import scala.concurrent.{ ExecutionContext, Future }

class SendOrderProjectionHandler(
    system: ActorSystem[_],
    orderService: ShoppingOrderService)
    extends Handler[EventEnvelope[ShoppingCart.Event]] {
  private val log = LoggerFactory.getLogger(getClass)
  private implicit val ec: ExecutionContext =
    system.executionContext

  private val sharding = ClusterSharding(system)
  implicit private val timeout: Timeout =
    Timeout.create(
      system.settings.config.getDuration("shopping-cart-service.ask-timeout"))

  override def process(
      envelope: EventEnvelope[ShoppingCart.Event]): Future[Done] = {
    envelope.event match {
      case checkout: ShoppingCart.CheckedOut =>
        sendOrder(checkout)

      case _ =>
        // this projection is only interested in CheckedOut events
        Future.successful(Done)
    }

  }

  private def sendOrder(checkout: ShoppingCart.CheckedOut): Future[Done] = {
    val entityRef =
      sharding.entityRefFor(ShoppingCart.EntityKey, checkout.cartId)
    entityRef.ask(ShoppingCart.Get).flatMap { cart =>
      val items =
        cart.items.iterator.map { case (itemId, quantity) =>
          Item(itemId, quantity)
        }.toList
      log.info(
        "Sending order of {} items for cart {}.",
        items.size,
        checkout.cartId)
      val orderReq = OrderRequest(checkout.cartId, items)
      orderService.order(orderReq).map(_ => Done)
    }
  }

}
