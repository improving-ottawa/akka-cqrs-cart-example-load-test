package shopping.cart

import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.projection.eventsourced.scaladsl.EventSourcedProvider

object CassandraJournalSourceFactory {

  val sourceFactory: shopping.cart.ItemPopularityProjection.SourceFactory = (tag, system) =>
    EventSourcedProvider.eventsByTag[ShoppingCart.Event](system, CassandraReadJournal.Identifier, tag)

}
