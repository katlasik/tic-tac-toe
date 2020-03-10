import sbt._

object Dependencies {
  val CatsVersion = "2.1.0"
  val Http4sVersion = "0.21.0"
  val CirceVersion = "0.13.0"
  val MouseVersion = "0.24"
  val PureConfigVersion = "0.12.2"
  val LogbackVersion = "1.2.3"
  val EnumeratumCirceVersion = "1.5.22"
  val EnumeratumDoobieVersion = "1.5.17"
  val TapirVersion = "0.12.11"
  val BetterFilesVersion = "3.8.0"
  val CatsTimeVersion = "0.3.0"
  val DoobieVersion = "0.8.8"
  val TSecVersion = "0.2.0"
  val HenkanVersion = "0.6.4"
  val FlywayVersion = "6.1.1"
  val ScalateVersion = "1.9.5"
  val JavaxMailVersion = "1.4.7"

  val ScalaTestVersion = "3.0.8"
  val ScalaCheckVersion = "1.14.3"
  val SttpVersion = "1.7.2"
  val ParallelCollectionsVersion = "0.2.0"

  val TestContainersScalaVersion = "0.33.0"

  lazy val dependencies = Seq(
    "org.typelevel" %% "cats-core" % CatsVersion,
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-circe" % Http4sVersion,
    "org.http4s" %% "http4s-dsl" % Http4sVersion,
    "org.typelevel" %% "mouse" % MouseVersion,
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-literal" % CirceVersion,
    "io.circe" %% "circe-generic-extras" % CirceVersion,
    "io.circe" %% "circe-parser" % CirceVersion,
    "io.circe" %% "circe-optics" % CirceVersion,
    "io.github.jmcardon" %% "tsec-http4s" % TSecVersion,
    "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
    "javax.mail" % "mail" % JavaxMailVersion,
    "io.chrisdavenport" %% "cats-time" % CatsTimeVersion,
    "ch.qos.logback" % "logback-classic" % LogbackVersion,
    "com.beachape" %% "enumeratum-circe" % EnumeratumCirceVersion,
    "com.beachape" %% "enumeratum-doobie" % EnumeratumDoobieVersion,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
    "org.scalacheck" %% "scalacheck" % ScalaCheckVersion % Test,
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % TapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % TapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % TapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % TapirVersion,
    "com.kailuowang" %% "henkan-convert" % HenkanVersion,
    "org.scalatra.scalate" %% "scalate-core" % ScalateVersion,
    "com.github.pathikrit" %% "better-files" % BetterFilesVersion,
    "org.tpolecat" %% "doobie-core" % DoobieVersion,
    "org.tpolecat" %% "doobie-scalatest" % DoobieVersion,
    "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
    "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
    "org.flywaydb" % "flyway-core" % FlywayVersion,
    "org.scalatest" %% "scalatest" % ScalaTestVersion % IntegrationTest,
    "com.softwaremill.sttp" %% "core" % SttpVersion % IntegrationTest,
    "org.scala-lang.modules" %% "scala-parallel-collections" % ParallelCollectionsVersion % IntegrationTest,
    "com.dimafeng" %% "testcontainers-scala" % TestContainersScalaVersion % IntegrationTest
  )
}
