package shopping.cart

import com.typesafe.config.ConfigFactory
import org.scalatest.wordspec.AnyWordSpec

class LocalConfigSpec extends AnyWordSpec {

  "when doin it"  when {
    "in" in {
      val c = ConfigFactory.load("local1.conf").getConfig("akka-persistence-jdbc")
      println(c)
    }
  }

}
