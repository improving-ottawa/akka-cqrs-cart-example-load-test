
package shopping.cart

import akka.Done
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import org.slf4j.LoggerFactory
import shopping.cart.repository.CassandraItemPopularityRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CassandraItemPopularityProjectionHandler(
    tag: String,
    repo: CassandraItemPopularityRepository)
    extends Handler[EventEnvelope[ShoppingCart.Event]]() {

  private val log = LoggerFactory.getLogger(getClass)

  override def process(
      envelope: EventEnvelope[ShoppingCart.Event]): Future[Done] = {
    envelope.event match {
      case ShoppingCart.ItemAdded(_, itemId, quantity) =>
        update(itemId, quantity)

      case ShoppingCart.ItemQuantityAdjusted(
            _,
            itemId,
            newQuantity,
            oldQuantity) =>
        update(itemId, newQuantity - oldQuantity)

      case ShoppingCart.ItemRemoved(_, itemId, oldQuantity) =>
        update(itemId, 0 - oldQuantity)

      case _: ShoppingCart.CheckedOut => Future.successful(Done)
    }
  }

  private def update(itemId: String, quantity: Int) = {
    (for {
      _ <- repo.update(itemId, quantity)
      r <- logItemCount(itemId)
    } yield r).mapTo[Done]
  }

  private def logItemCount(
      itemId: String) = {
    repo.getItem(itemId)
      .map(_.getOrElse(0))
      .map { result =>
        log.info(
          "ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]",
          tag,
          itemId,
          result
        )
      }
  }

}

