package shopping.cart.repository

import akka.actor.typed.ActorSystem
import akka.stream.alpakka.cassandra.CassandraWriteSettings
import akka.stream.alpakka.cassandra.scaladsl.{CassandraFlow, CassandraSession, CassandraSource}
import akka.stream.scaladsl.{Sink, Source}
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}

import scala.concurrent.Future

trait CassandraItemPopularityRepository {
  def update(session: ScalikeJdbcSession, itemId: String, delta: Int): Future[Unit]
  def getItem(session: ScalikeJdbcSession, itemId: String): Future[Option[Long]]
}

class CassandraItemPopularityRepositoryImpl(cassandraSession: CassandraSession)(
    implicit actorSystem: ActorSystem[_])
    extends CassandraItemPopularityRepository {

  val statementBinder: (String, PreparedStatement) => BoundStatement =
    (itemId, preparedStatement) => preparedStatement.bind(itemId)
  override def update(
      session: ScalikeJdbcSession,
      itemId: String,
      delta: Int): Future[Unit] = {
    Source(Seq(itemId))
      .via(
        CassandraFlow.create(
          CassandraWriteSettings.defaults,
          s"UPDATE cart_service.item_popularity SET count = count + ? where item_id = ?",
          statementBinder)(cassandraSession)
      )
      .runWith(Sink.ignore)
      .map(_ => ())(actorSystem.executionContext)
  }

  override def getItem(
      session: ScalikeJdbcSession,
      itemId: String): Future[Option[Long]] = {
    CassandraSource(s"SELECT count FROM cart_service.item_popularity WHERE id = ?", itemId)(cassandraSession)
      .map(_.getLong("count"))
      .map {
        case 0 => None
        case c => Some(c)
      }
      .runWith(Sink.head[Option[Long]])
  }
}
