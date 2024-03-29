import sbt._

object Dependencies {
  object V {
    val akka = "2.7.0"
  }

  val integrationTestDependencies = Seq(
    "com.typesafe.akka" %% "akka-discovery" % V.akka,
    "com.typesafe.akka" %% "akka-protobuf" % V.akka,
    "com.typesafe.akka" %% "akka-stream" % V.akka,
    "org.scalacheck" %% "scalacheck" % "1.17.0",
    "com.lightbend.akka.samples" %% "shopping-cart-service" % "0.2.0-SNAPSHOT"
  )
}
