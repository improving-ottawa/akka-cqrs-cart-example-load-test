package shopping.cart.repository.jdbc

import akka.actor.typed.{ActorSystem, DispatcherSelector}
import scalikejdbc._
import shopping.cart.repository.ItemPopularityRepository
import shopping.cart.repository.jdbc.JdbcItemPopularityRepositoryFactory.JdbcItemPopularityRepositoryFactory

import scala.concurrent.{ExecutionContext, Future}

class JdbcItemPopularityRepository(
    repoFactory: JdbcItemPopularityRepositoryFactory,
    sessionFactory: () => ScalikeJdbcSession = () => new ScalikeJdbcSession())(implicit val ec: ExecutionContext)
    extends ItemPopularityRepository {
  def update(itemId: String, delta: Int): Future[Unit] = {
    val session = sessionFactory()
    repoFactory(session)
      .update(itemId, delta)
      .andThen(_ => session.close())
  }

  def getItem(itemId: String): Future[Option[Long]] = {
    val session = sessionFactory()
    repoFactory(session)
      .getItem(itemId)
      .andThen(_ => session.close())
  }
}


class JdbcItemPopularityRepositoryFactoryImpl(
    blockingJdbcExecutor: ExecutionContext)
    extends JdbcItemPopularityRepositoryFactory {

  def apply(session: ScalikeJdbcSession): ItemPopularityRepository = new ItemPopularityRepository {
    override def update(itemId: String, delta: Int): Future[Unit] = {
      Future {
        session.db.withinTx { implicit dbSession =>
          // This uses the PostgreSQL `ON CONFLICT` feature
          // Alternatively, this can be implemented by first issuing the `UPDATE`
          // and checking for the updated rows count. If no rows got updated issue
          // the `INSERT` instead.
          sql"""
           INSERT INTO item_popularity (itemid, count) VALUES ($itemId, $delta)
           ON CONFLICT (itemid) DO UPDATE SET count = item_popularity.count + $delta
         """.executeUpdate().apply()
        }
        ()
      }(blockingJdbcExecutor)
    }

    override def getItem(itemId: String): Future[Option[Long]] = {
      Future {
        if (session.db.isTxAlreadyStarted) {
          session.db.withinTx { implicit dbSession =>
            select(itemId)
          }
        } else {
          session.db.readOnly { implicit dbSession =>
            select(itemId)
          }
        }
      }(blockingJdbcExecutor)
    }

    private def select(itemId: String)(implicit dbSession: DBSession) = {
      sql"SELECT count FROM item_popularity WHERE itemid = $itemId"
        .map(_.long("count"))
        .toOption()
        .apply()
    }
  }
}

object JdbcItemPopularityRepositoryFactory {
  type JdbcItemPopularityRepositoryFactory =
    ScalikeJdbcSession => ItemPopularityRepository
  def apply(ec: ExecutionContext): JdbcItemPopularityRepositoryFactory = new JdbcItemPopularityRepositoryFactoryImpl(ec)
  def blockingJdbcExecutor(system: ActorSystem[_]): ExecutionContext =
    system.dispatchers.lookup(
      DispatcherSelector.fromConfig(
        "akka.projection.jdbc.blocking-jdbc-dispatcher"))
}
