
package shopping.cart

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.cassandra.query.javadsl.CassandraReadJournal
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.projection.scaladsl.{ExactlyOnceProjection, SourceProvider}
import akka.projection.{Projection, ProjectionBehavior, ProjectionId}
import shopping.cart.repository.{CassandraItemPopularityRepository, JdbcItemPopularityRepository, ScalikeJdbcSession}

object CassandraItemPopularityProjection {

  def init(
      system: ActorSystem[_],
      repository: CassandraItemPopularityRepository): Unit = {
    ShardedDaemonProcess(system).init(
      name = "ItemPopularityProjection",
      ShoppingCart.tags.size,
      index =>
        ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }


  private def createProjectionFor(
                                   system: ActorSystem[_],
                                   repository: CassandraItemPopularityRepository,
                                   index: Int)
      : Projection[EventEnvelope[ShoppingCart.Event]] = {
    val tag = ShoppingCart.tags(index)

    val sourceProvider
        : SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]] =
      EventSourcedProvider.eventsByTag[ShoppingCart.Event](
        system = system,
        readJournalPluginId = CassandraReadJournal.Identifier,
        tag = tag)


    CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("ItemPopularityProjection", tag),
      sourceProvider,
      handler = () =>
        new CassandraItemPopularityProjectionHandler(tag, repository),
      )
  }

}

