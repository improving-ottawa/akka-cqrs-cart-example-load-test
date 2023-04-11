package shopping.cart.projection

import akka.Done
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.Handler
import org.slf4j.LoggerFactory
import shopping.cart.ShoppingCart
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
      case ShoppingCart.ItemAdded(_, itemId, quantity, _) =>
        update(itemId, quantity)

      case ShoppingCart.ItemQuantityAdjusted(
            _,
            itemId,
            newQuantity,
            oldQuantity,
            _) =>
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
    } yield r).map(_ => Done)
  }

  private def logItemCount(itemId: String) = {
    repo.getItem(itemId).map(_.getOrElse(0)).map { result =>
      log.info(
        "ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]",
        tag,
        itemId,
        result)
    }
  }

}

object CassandraItemPopularityProjectionHandler {
  def cassandraProjectionFactory(repo: CassandraItemPopularityRepository)
      : ItemPopularityProjection.ProjectionFactory = {
    (projectionId, sourceProvider, tag) =>
      CassandraProjection
        .atLeastOnce[Offset, EventEnvelope[ShoppingCart.Event]](
          projectionId = projectionId,
          sourceProvider,
          handler =
            () => new CassandraItemPopularityProjectionHandler(tag, repo))
  }

  val sourceFactory: ItemPopularityProjection.SourceFactory =
    (tag, system) =>
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system,
        CassandraReadJournal.Identifier,
        tag)
}
