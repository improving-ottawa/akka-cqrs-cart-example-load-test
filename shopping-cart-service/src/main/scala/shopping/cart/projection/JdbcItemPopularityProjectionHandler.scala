package shopping.cart.projection

import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.{ JdbcHandler, JdbcProjection }
import org.slf4j.LoggerFactory
import shopping.cart.ShoppingCart
import shopping.cart.projection.ItemPopularityProjection.SourceFactory
import shopping.cart.repository.jdbc.JdbcItemPopularityRepositoryFactory.JdbcItemPopularityRepositoryFactory
import shopping.cart.repository.jdbc.ScalikeJdbcSession

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, Future }

// TODO:  refactor to just be a Handler[EventEnvelop[ShoppingCart.Event]] will allow to collapse with
//  [[CassandraItemPopularityProjectionHandler]] into something generic (and get rid of that nasty Await!)
class JdbcItemPopularityProjectionHandler(
    tag: String,
    repoFactory: JdbcItemPopularityRepositoryFactory)
    extends JdbcHandler[
      EventEnvelope[ShoppingCart.Event],
      ScalikeJdbcSession]() {

  private val log = LoggerFactory.getLogger(getClass)

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[ShoppingCart.Event]): Unit = {
    val futUnit: Future[Unit] = envelope.event match {
      case ShoppingCart.ItemAdded(_, itemId, quantity, _) =>
        update(session, itemId, quantity)

      case ShoppingCart.ItemQuantityAdjusted(
            _,
            itemId,
            newQuantity,
            oldQuantity,
            _) =>
        update(session, itemId, newQuantity - oldQuantity)

      case ShoppingCart.ItemRemoved(_, itemId, oldQuantity) =>
        update(session, itemId, 0 - oldQuantity)

      case _: ShoppingCart.CheckedOut => Future.successful(())
    }
    // Look away!  not clear why this method signature is synchronous as this is almost always going to be an IO operation
    Await.result(futUnit, 10.seconds)
  }

  private def update(
      session: ScalikeJdbcSession,
      itemId: String,
      quantity: Int): Future[Unit] = {
    for {
      _ <- repoFactory(session).update(itemId, quantity)
      i <- logItemCount(session, itemId)
    } yield i
  }

  private def logItemCount(
      session: ScalikeJdbcSession,
      itemId: String): Future[Unit] = {
    repoFactory(session)
      .getItem(itemId)
      .map(_.getOrElse(0))
      .andThen {
        case scala.util.Success(res) =>
          log.info(
            "ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]",
            tag,
            itemId,
            res)
        case scala.util.Failure(e) => throw e
      }
      .map(_ => ())
  }

}

object JdbcItemPopularityProjectionHandler {
  def jdbcProjectionFactory(
      repoFactory: JdbcItemPopularityRepositoryFactory,
      system: ActorSystem[_]): ItemPopularityProjection.ProjectionFactory = {
    (projectionId, sourceProvider, tag) =>
      JdbcProjection.exactlyOnce(
        projectionId = projectionId,
        sourceProvider,
        handler =
          () => new JdbcItemPopularityProjectionHandler(tag, repoFactory),
        sessionFactory = () => new ScalikeJdbcSession())(system)
  }

  def jdbcReadJournalSourceFactory: SourceFactory = (tag, system) =>
    EventSourcedProvider.eventsByTag[ShoppingCart.Event](
      system = system,
      readJournalPluginId = JdbcReadJournal.Identifier,
      tag = tag)
}
