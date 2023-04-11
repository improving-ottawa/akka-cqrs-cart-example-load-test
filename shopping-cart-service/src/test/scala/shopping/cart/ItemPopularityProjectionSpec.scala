package shopping.cart

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.query.Offset
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import akka.projection.testkit.scaladsl.{
  ProjectionTestKit,
  TestProjection,
  TestSourceProvider
}
import akka.stream.scaladsl.Source
import org.scalatest.wordspec.AnyWordSpecLike
import shopping.cart.projection.JdbcItemPopularityProjectionHandler
import shopping.cart.repository.ItemPopularityRepository

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

object ItemPopularityProjectionSpec {
  // stub out the db layer and simulate recording item count updates
  class TestItemPopularityRepository extends ItemPopularityRepository {
    var counts: Map[String, Long] = Map.empty

    override def update(itemId: String, delta: Int): Future[Unit] = {
      counts = counts + (itemId -> (counts.getOrElse(itemId, 0L) + delta))
      Future.successful(())
    }

    override def getItem(itemId: String): Future[Option[Long]] =
      Future.successful(counts.get(itemId))
  }
}

class ItemPopularityProjectionSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {
  import ItemPopularityProjectionSpec.TestItemPopularityRepository

  private val projectionTestKit = ProjectionTestKit(system)

  private def createEnvelope(
      event: ShoppingCart.Event,
      seqNo: Long,
      timestamp: Long = 0L) =
    EventEnvelope(
      Offset.sequence(seqNo),
      "persistenceId",
      seqNo,
      event,
      timestamp)

  private def toAsyncHandler(itemHandler: JdbcItemPopularityProjectionHandler)(
      implicit
      ec: ExecutionContext): Handler[EventEnvelope[ShoppingCart.Event]] =
    eventEnvelope =>
      Future {
        itemHandler.process(session = null, eventEnvelope)
        Done
      }

  "The events from the Shopping Cart" should {

    "update item popularity counts by the projection" in {

      val events =
        Source(
          List[EventEnvelope[ShoppingCart.Event]](
            createEnvelope(
              ShoppingCart
                .ItemAdded("a7098", "bowling shoes", 1, new Array[Byte](0)),
              0L),
            createEnvelope(
              ShoppingCart.ItemQuantityAdjusted(
                "a7098",
                "bowling shoes",
                2,
                1,
                new Array[Byte](0)),
              1L),
            createEnvelope(
              ShoppingCart.CheckedOut(
                "a7098",
                Instant.parse("2020-01-01T12:00:00.00Z"),
                new Array[Byte](0)),
              2L),
            createEnvelope(
              ShoppingCart
                .ItemAdded("0d12d", "akka t-shirt", 1, new Array[Byte](0)),
              3L),
            createEnvelope(
              ShoppingCart.ItemAdded("0d12d", "skis", 1, new Array[Byte](0)),
              4L),
            createEnvelope(ShoppingCart.ItemRemoved("0d12d", "skis", 1), 5L),
            createEnvelope(
              ShoppingCart.CheckedOut(
                "0d12d",
                Instant.parse("2020-01-01T12:05:00.00Z"),
                new Array[Byte](0)),
              6L)))

      val repository = new TestItemPopularityRepository
      val projectionId =
        ProjectionId("item-popularity", "carts-0")
      val sourceProvider =
        TestSourceProvider[Offset, EventEnvelope[ShoppingCart.Event]](
          events,
          extractOffset = env => env.offset)
      val projection =
        TestProjection[Offset, EventEnvelope[ShoppingCart.Event]](
          projectionId,
          sourceProvider,
          () =>
            toAsyncHandler(
              new JdbcItemPopularityProjectionHandler(
                "carts-0",
                _ => repository))(system.executionContext))

      projectionTestKit.run(projection) {
        repository.counts shouldBe Map(
          "bowling shoes" -> 2,
          "akka t-shirt" -> 1,
          "skis" -> 0)
      }
    }
  }

}
