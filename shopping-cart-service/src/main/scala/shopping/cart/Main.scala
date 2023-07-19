package shopping.cart

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.alpakka.cassandra.CassandraSessionSettings
import akka.stream.alpakka.cassandra.scaladsl.{
  CassandraSession,
  CassandraSessionRegistry
}
import org.slf4j.LoggerFactory
import shopping.cart.projection.{
  CassandraItemPopularityProjectionHandler,
  ItemPopularityProjection
}
import shopping.cart.repository.CassandraItemPopularityRepository
import shopping.order.proto.{ ShoppingOrderService, ShoppingOrderServiceClient }

import scala.util.control.NonFatal

object Main {

  val logger = LoggerFactory.getLogger("shopping.cart.Main")

  def main(args: Array[String]): Unit = {
    val system =
      ActorSystem[Nothing](Behaviors.empty, "ShoppingCartService")
    try {
      val orderService = orderServiceClient(system)
      init(system, orderService)
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization failure.", e)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_], orderService: ShoppingOrderService): Unit = {
    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

    ShoppingCart.init(system)

    val config = system.settings.config

//    // JDBC
//    // construct some jdbc primitives
//    ScalikeJdbcSetup.init(system)
//    val jdbcBlockingExecutor: ExecutionContext = JdbcItemPopularityRepositoryFactory.blockingJdbcExecutor(system)
//    val jdbcRepoFactory = JdbcItemPopularityRepositoryFactory(jdbcBlockingExecutor)
//
//    val popularityRepo: ItemPopularityRepository = new JdbcItemPopularityRepository(jdbcRepoFactory)

//    // read event journal from jdbc to source popularity projection
//    val readJournalSourceFactory: ItemPopularityProjection.SourceFactory =
//      JdbcItemPopularityProjectionHandler.jdbcReadJournalSourceFactory

//    // project event journal to jdbc projection
//    val projectionFactory: ItemPopularityProjection.ProjectionFactory =
//      JdbcItemPopularityProjectionHandler.jdbcProjectionFactory(jdbcRepoFactory, system)

    // Cassandra
    // read event journal from Cassandra to source popularity projection
    val readJournalSourceFactory: ItemPopularityProjection.SourceFactory =
      CassandraItemPopularityProjectionHandler.sourceFactory

    val sessionSettings = CassandraSessionSettings()
    val cassandraSession: CassandraSession =
      CassandraSessionRegistry.get(system).sessionFor(sessionSettings)

    val popularityRepo = new CassandraItemPopularityRepository(
      cassandraSession)(system)

    val runProjection = config.getBoolean("shopping-cart-service.run-item-popularity-projection")
    if (runProjection) {
      // project event journal to cassandra projection
      val projectionFactory: ItemPopularityProjection.ProjectionFactory =
        CassandraItemPopularityProjectionHandler.cassandraProjectionFactory(
          popularityRepo)

      ItemPopularityProjection.init(
        system,
        readJournalSourceFactory,
        projectionFactory)
    }

    // disable kafka domain event projection: goal is to minimize moving parts involved in a load test
    //PublishEventsProjection.init(system)

    // disable projection to order service over grpc for the same reasons as above
    //SendOrderProjection.init(system, orderService)

    val grpcInterface =
      system.settings.config.getString("shopping-cart-service.grpc.interface")
    val grpcPort =
      system.settings.config.getInt("shopping-cart-service.grpc.port")
    val grpcService =
      new ShoppingCartServiceImpl(system, popularityRepo)
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService)

  }

  protected def orderServiceClient(
      system: ActorSystem[_]): ShoppingOrderService = {
    val orderServiceClientSettings =
      GrpcClientSettings
        .connectToServiceAt(
          system.settings.config.getString("shopping-order-service.host"),
          system.settings.config.getInt("shopping-order-service.port"))(system)
        .withTls(false)
    ShoppingOrderServiceClient(orderServiceClientSettings)(system)
  }
}
