package shopping.cart.repository

import akka.actor.typed.ActorSystem
import akka.stream.alpakka.cassandra.CassandraWriteSettings
import akka.stream.alpakka.cassandra.scaladsl.{
  CassandraFlow,
  CassandraSession,
  CassandraSource
}
import akka.stream.scaladsl.{ Sink, Source }
import com.datastax.oss.driver.api.core.cql.{
  BoundStatement,
  PreparedStatement
}

import scala.concurrent.Future

class CassandraItemPopularityRepository(cassandraSession: CassandraSession)(
    implicit actorSystem: ActorSystem[_])
    extends ItemPopularityRepository {

  val statementBinder: (Update, PreparedStatement) => BoundStatement =
    (update, preparedStatement) =>
      preparedStatement.bind(update.delta, update.itemId)

  case class Update(itemId: String, delta: Long)

  override def update(itemId: String, delta: Int): Future[Unit] = {
    Source(Seq(Update(itemId, delta.toLong)))
      .via(
        CassandraFlow.create(
          CassandraWriteSettings.defaults,
          s"UPDATE cart_service.item_popularity SET count = count + ? where item_id = ?",
          statementBinder)(cassandraSession))
      .runWith(Sink.ignore)
      .map(_ => ())(actorSystem.executionContext)
  }

  override def getItem(itemId: String): Future[Option[Long]] = {
    CassandraSource(
      s"SELECT count FROM cart_service.item_popularity WHERE item_id = ?",
      itemId)(cassandraSession)
      .map(_.getLong("count"))
      .fold(Option.empty[Long]) {
        case (None, count) if count == 0 => None
        case (_, count)                  => Some(count)
      }
      .runWith(Sink.head[Option[Long]])
  }
}
