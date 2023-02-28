import sbt._

object Dependencies {
  object V {
    val akka = "2.7.0"
  }

  val integrationTestDependencies = Seq(
    "com.typesafe.akka" %% "akka-discovery" % V.akka % Test,
    "com.typesafe.akka" %% "akka-protobuf" % V.akka % Test,
    "com.typesafe.akka" %% "akka-stream" % V.akka % Test,
    "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
    "com.lightbend.akka.samples" %% "shopping-cart-service" % "0.1.0-SNAPSHOT"
  )
}
