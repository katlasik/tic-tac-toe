package io.tictactoe.testutils

import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS

import cats.syntax.option._
import com.dimafeng.testcontainers.GenericContainer
import io.tictactoe.database.{DatabaseConfig, DbUrlParser}
import org.testcontainers.containers.wait.strategy.{LogMessageWaitStrategy, Wait}

import cats.syntax.either._

trait Containers { _: ItTest =>

  lazy protected val Right(DatabaseConfig(dbUsername, dbPassword, _, dbName, _, host)) = DbUrlParser.parse(config.db.databaseUrl)

  lazy val mailContainer: GenericContainer = new GenericContainer(
    dockerImage = "mailhog/mailhog:v1.0.0",
    exposedPorts = List(8025, 1025),
    waitStrategy = Wait
      .forHttp("/")
      .withStartupTimeout(Duration.ofSeconds(30))
      .some
  )

  lazy val dbContainer: GenericContainer = new GenericContainer(
    dockerImage = "postgres:12",
    exposedPorts = List(5432),
    command = Seq("postgres", "-c", "fsync=off"),
    env = Map(
      "POSTGRES_DB" -> dbName,
      "POSTGRES_USER" -> dbUsername,
      "POSTGRES_PASSWORD" -> dbPassword
    ),
    waitStrategy = new LogMessageWaitStrategy()
      .withRegEx(".*database system is ready to accept connections.*\\s")
      .withTimes(2)
      .withStartupTimeout(Duration.of(60, SECONDS))
      .some
  )

  private lazy val containers = List(mailContainer, dbContainer)

  def startContainers(): Unit = containers.foreach(_.start())

  def stopContainers(): Unit = containers.foreach(_.stop())

  def withContainers[A](body: => A): A = if(Either.catchNonFatal(sys.env("IT_TEST_REUSE_CONTAINERS")).isLeft) {
    try {
      startContainers()
      body
    } finally  {
      stopContainers()
    }
  } else {
    body
  }

}
