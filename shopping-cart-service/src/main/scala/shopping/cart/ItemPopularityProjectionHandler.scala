
package shopping.cart

import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.scaladsl.JdbcHandler
import org.slf4j.LoggerFactory
import shopping.cart.repository.{ ItemPopularityRepository, ScalikeJdbcSession }

class ItemPopularityProjectionHandler(
    tag: String,
    system: ActorSystem[_],
    repoFactory: ScalikeJdbcSession => ItemPopularityRepository)
    extends JdbcHandler[
      EventEnvelope[ShoppingCart.Event],
      ScalikeJdbcSession]() {

  private val log = LoggerFactory.getLogger(getClass)

  override def process(
      session: ScalikeJdbcSession,
      envelope: EventEnvelope[ShoppingCart.Event]): Unit = {
    envelope.event match {
      case ShoppingCart.ItemAdded(_, itemId, quantity) =>
        repoFactory(session).update(itemId, quantity)
        logItemCount(session, itemId)


      case ShoppingCart.ItemQuantityAdjusted(
            _,
            itemId,
            newQuantity,
            oldQuantity) =>
        repoFactory(session).update(itemId, newQuantity - oldQuantity)
        logItemCount(session, itemId)

      case ShoppingCart.ItemRemoved(_, itemId, oldQuantity) =>
        repoFactory(session).update(itemId, 0 - oldQuantity)
        logItemCount(session, itemId)



      case _: ShoppingCart.CheckedOut =>
    }
  }

  private def logItemCount(
      session: ScalikeJdbcSession,
      itemId: String): Unit = {
    log.info(
      "ItemPopularityProjectionHandler({}) item popularity for '{}': [{}]",
      tag,
      itemId,
      repoFactory(session).getItem(itemId).getOrElse(0))
  }

}

