package shopping.cart

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import org.slf4j.LoggerFactory
import shopping.cart.repository.ItemPopularityRepository
import shopping.cart.repository.jdbc.{JdbcItemPopularityRepository, JdbcItemPopularityRepositoryFactory, ScalikeJdbcSetup}
import shopping.order.proto.{ShoppingOrderService, ShoppingOrderServiceClient}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
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

    ScalikeJdbcSetup.init(system)

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

    ShoppingCart.init(system)

    // construct some jdbc primitives
    val jdbcBlockingExecutor: ExecutionContext = JdbcItemPopularityRepositoryFactory.blockingJdbcExecutor(system)
    val jdbcRepoFactory = JdbcItemPopularityRepositoryFactory(jdbcBlockingExecutor)

    val popularityRepo: ItemPopularityRepository = new JdbcItemPopularityRepository(jdbcRepoFactory)

    // read event journal from jdbc to source projection
    val readJournalSourceFactory: ItemPopularityProjection.SourceFactory =
      JdbcItemPopularityProjectionHandler.jdbcReadJournalSourceFactory

    // project event journal to jdbc projection
    val projectionFactory: ItemPopularityProjection.ProjectionFactory =
      JdbcItemPopularityProjectionHandler.jdbcProjectionFactory(jdbcRepoFactory, system)

    ItemPopularityProjection.init(system, readJournalSourceFactory, projectionFactory)

    // disable kafka domain event projection: goal is to minimize moving parts involved in a load test
    //PublishEventsProjection.init(system)


    SendOrderProjection.init(system, orderService)

    SendOrderProjection.init(system, orderService)

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
