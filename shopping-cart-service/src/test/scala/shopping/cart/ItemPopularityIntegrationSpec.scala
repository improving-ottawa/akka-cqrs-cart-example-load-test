package shopping.cart

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{ Cluster, Join }
import akka.persistence.testkit.scaladsl.PersistenceInit
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.OptionValues
import org.scalatest.wordspec.AnyWordSpecLike
import shopping.cart.projection.{
  ItemPopularityProjection,
  JdbcItemPopularityProjectionHandler
}
import shopping.cart.repository.jdbc.{
  JdbcItemPopularityRepositoryFactory,
  ScalikeJdbcSession,
  ScalikeJdbcSetup
}

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

object ItemPopularityIntegrationSpec {
  val config: Config =
    ConfigFactory.load("item-popularity-integration-test.conf")
}

class ItemPopularityIntegrationSpec
    extends ScalaTestWithActorTestKit(ItemPopularityIntegrationSpec.config)
    with AnyWordSpecLike
    with OptionValues {

  private val ec =
    JdbcItemPopularityRepositoryFactory.blockingJdbcExecutor(system)
  private lazy val itemPopularityRepositoryFactory =
    JdbcItemPopularityRepositoryFactory(ec)

  override protected def beforeAll(): Unit = {
    ScalikeJdbcSetup.init(system)
    CreateTableTestUtils.dropAndRecreateTables(system)
    // avoid concurrent creation of keyspace and tables
    val timeout = 10.seconds
    Await.result(
      PersistenceInit.initializeDefaultPlugins(system, timeout),
      timeout)

    ShoppingCart.init(system)

    val jdbcProjectionFactory = JdbcItemPopularityProjectionHandler
      .jdbcProjectionFactory(itemPopularityRepositoryFactory, system) _
    val jdbcSourceFactory =
      JdbcItemPopularityProjectionHandler.jdbcReadJournalSourceFactory
    ItemPopularityProjection.init(
      system,
      jdbcSourceFactory,
      jdbcProjectionFactory)

    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
  }

  "Item popularity projection" should {
    "init and join Cluster" in {
      Cluster(system).manager ! Join(Cluster(system).selfMember.address)

      // let the node join and become Up
      eventually {
        Cluster(system).selfMember.status should ===(MemberStatus.Up)
      }
    }

    "consume cart events and update popularity count" in {
      val sharding = ClusterSharding(system)
      val cartId1 = "cart1"
      val cartId2 = "cart2"
      val item1 = "item1"
      val item2 = "item2"

      val cart1 = sharding.entityRefFor(ShoppingCart.EntityKey, cartId1)
      val cart2 = sharding.entityRefFor(ShoppingCart.EntityKey, cartId2)

      val reply1: Future[ShoppingCart.Summary] =
        cart1.askWithStatus(
          ShoppingCart.AddItem(item1, 3, new Array[Byte](0), _))
      reply1.futureValue.items.values.sum should ===(3)

      eventually {
        ScalikeJdbcSession.withSession { session =>
          itemPopularityRepositoryFactory(session)
            .getItem(item1)
            .futureValue should ===(Some(3))
        }
      }

      val reply2: Future[ShoppingCart.Summary] =
        cart1.askWithStatus(
          ShoppingCart.AddItem(item2, 5, new Array[Byte](0), _))
      reply2.futureValue.items.values.sum should ===(3 + 5)
      // another cart
      val reply3: Future[ShoppingCart.Summary] =
        cart2.askWithStatus(
          ShoppingCart.AddItem(item2, 4, new Array[Byte](0), _))
      reply3.futureValue.items.values.sum should ===(4)

      eventually {
        ScalikeJdbcSession.withSession { session =>
          itemPopularityRepositoryFactory(session)
            .getItem(item2)
            .futureValue should ===(Some(5 + 4))
          itemPopularityRepositoryFactory(session)
            .getItem(item1)
            .futureValue should ===(Some(3))
        }
      }
    }

  }
}
