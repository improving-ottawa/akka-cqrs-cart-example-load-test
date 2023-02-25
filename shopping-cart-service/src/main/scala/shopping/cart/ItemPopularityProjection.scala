package shopping.cart

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.SourceProvider
import akka.projection.{Projection, ProjectionBehavior, ProjectionId}

object ItemPopularityProjection {

  /**
   * `(tag, system) => SourceProvider[Offset, EventEnvelope[ShoppingCart.Event]]`
   */
  type SourceFactory = (String, ActorSystem[_]) => SourceProvider[
    Offset,
    EventEnvelope[ShoppingCart.Event]]

  /**
   * `(projectionId, sourceProvider, tag) => Projection[EventEnvelope[ShoppingCart.Event]]`
   */
  type ProjectionFactory = (
    ProjectionId,
      SourceProvider[
        Offset,
        EventEnvelope[ShoppingCart.Event]], String) => Projection[EventEnvelope[ShoppingCart.Event]]

  def init(
      system: ActorSystem[_],
      sourceFactory: SourceFactory,
      projectionFactory: ProjectionFactory): Unit = {
    ShardedDaemonProcess(system).init(
      name = "ItemPopularityProjection",
      ShoppingCart.tags.size,
      index =>
        ProjectionBehavior(createProjectionFor(system, sourceFactory, projectionFactory, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop))
  }

  private def createProjectionFor(
      system: ActorSystem[_],
      sourceFactory: SourceFactory,
      projectionFactory: ProjectionFactory,
      index: Int)
      : Projection[EventEnvelope[ShoppingCart.Event]] = {
    val tag = ShoppingCart.tags(index)

    projectionFactory(
      ProjectionId("ItemPopularityProjection", tag),
      sourceFactory(tag, system),
      tag
    )
  }

}
