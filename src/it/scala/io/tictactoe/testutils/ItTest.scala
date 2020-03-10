package io.tictactoe.testutils

import java.net.URI
import java.sql.DriverManager
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS

import cats.effect.IO
import com.dimafeng.testcontainers.GenericContainer
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend, Uri, sttp}
import io.tictactoe.EntryPoint
import io.tictactoe.configuration.{Configuration, ConfigurationProperties}
import io.tictactoe.database.{DatabaseConfig, DbUrlParser}
import org.scalatest.{Args, BeforeAndAfterAll, BeforeAndAfterEach, Status, Suite}
import org.testcontainers.containers.wait.strategy.{LogMessageWaitStrategy, Wait}
import cats.implicits._
import com.typesafe.config.ConfigFactory
import mouse.all._

import scala.io.Source
import scala.util.{Try, Using}

trait ItTest extends BeforeAndAfterAll with BeforeAndAfterEach { self: Suite =>
  val config: ConfigurationProperties = Configuration.load[IO].flatMap(_.access()).unsafeRunSync()

  private implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
  private val DatabaseConfig(username, password, _, name, _, host) = DbUrlParser.parse(config.db.databaseUrl).toOption.get

  private def connection(port: Int) = DriverManager.getConnection(s"jdbc:postgresql://$host:$port/$name", username, password)

  lazy val mailContainer: GenericContainer = new GenericContainer(
    dockerImage = "mailhog/mailhog:v1.0.0",
    exposedPorts = List(8025, 1025),
    waitStrategy = Wait.forHttp("/").some
  )

  lazy val dbContainer: GenericContainer = new GenericContainer(
    dockerImage = "postgres:12",
    exposedPorts = List(5432),
    command = Seq("postgres", "-c", "fsync=off"),
    env = Map(
      "POSTGRES_DB" -> name,
      "POSTGRES_USER" -> username,
      "POSTGRES_PASSWORD" -> password
    ),
    waitStrategy = new LogMessageWaitStrategy()
      .withRegEx(".*database system is ready to accept connections.*\\s")
      .withTimes(2)
      .withStartupTimeout(Duration.of(60, SECONDS))
      .some
  )

  def dbPort: Int = Try(sys.env("IT_TEST_DB_PORT").toInt).getOrElse(dbContainer.mappedPort(5432))

  def mailSmtpPort: Int = Try(sys.env("IT_TEST_MAIL_SMTP_PORT").toInt).getOrElse(mailContainer.mappedPort(1025))

  def mailRestPort: Int = Try(sys.env("IT_TEST_MAIL_REST_PORT").toInt).getOrElse(mailContainer.mappedPort(8025))

  def baseUrl(url: String) = s"http://${config.server.host}:${config.server.port}/$url"

  abstract override def run(testName: Option[String], args: Args): Status = {

    val startContainer = Try(sys.env("IT_TEST_REUSE_CONTAINERS")).isFailure

    if (startContainer) {
      mailContainer.start()
      dbContainer.start()
    }
    updateConfiguration()
    try {
      val cancel = EntryPoint.run(Nil).unsafeRunCancelable(System.err.println)
      waitForServer()
      val status = super.run(testName, args)
      cancel.unsafeRunSync()
      status
    } finally {
      if (startContainer) {
        mailContainer.stop()
        dbContainer.stop()
      }
    }
  }

  def repeatUntil[R](times: Int = 5, failureMsg: String = "Condition was not fulfilled in configured time.")(predicate: => Option[R]): R = {
    predicate match {
      case None =>
        if (times > 0) {
          Thread.sleep(1000)
          repeatUntil(times - 1, failureMsg)(predicate)
        } else {
          fail(failureMsg)
        }
      case Some(r) => r
    }
  }

  def waitForServer(): Unit = repeatUntil(5, "Couldn't connect to server.") {
    val response = Try(sttp.get(Uri(host = config.server.host, port = config.server.port)).send())
    response.isSuccess.option(())
  }

  def updateConfiguration(): Unit = {
    System.setProperty("db.database-url", s"postgres://tictactoe_user:tictactoe@localhost:$dbPort/tictactoe")
    System.setProperty("mail.server.smtp.port", mailSmtpPort.toString)
    ConfigFactory.invalidateCaches()
  }

  def get(uri: String, headers: Map[String, String] = Map.empty): Result = {
    Result(
      sttp
        .get(Uri.apply(URI.create(uri)))
        .headers(headers)
        .send()
    )
  }

  def post(uri: String, json: String = "", headers: Map[String, String] = Map.empty): Result = {
    Result(
      sttp
        .post(Uri.apply(URI.create(uri)))
        .headers(headers)
        .body(json)
        .send()
    )
  }

  def put(uri: String, json: String = "", headers: Map[String, String] = Map.empty): Result = {
    Result(
      sttp
        .put(Uri.apply(URI.create(uri)))
        .headers(headers)
        .body(json)
        .send()
    )
  }

  def sql(scripts: String*): Unit = {

    val statement = connection(dbPort).createStatement()

    scripts.toList
      .foreach(f => Using(Source.fromFile(s"src/it/resources/scripts/$f"))(l => statement.execute(l.mkString)))
  }

}
