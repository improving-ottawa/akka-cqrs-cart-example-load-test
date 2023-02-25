package shopping.cart.repository

import scala.concurrent.Future
trait ItemPopularityRepository {
  def update(itemId: String, delta: Int): Future[Unit]

  def getItem(itemId: String): Future[Option[Long]]
}
