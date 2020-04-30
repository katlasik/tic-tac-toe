package io.tictactoe.testutils

import cats.effect.IO
import io.tictactoe.EntryPoint
import io.tictactoe.configuration.ConfigurationProperties
import org.scalatest.{Args, BeforeAndAfterAll, BeforeAndAfterEach, Retries, Status, Suite}
import com.typesafe.config.ConfigFactory
import io.tictactoe.infrastructure.configuration.Configuration

trait ItTest extends BeforeAndAfterAll with BeforeAndAfterEach with Containers with Database with Server with Http with Mails with Retries {
  self: Suite =>

  lazy val config: ConfigurationProperties = Configuration.load[IO].flatMap(_.access()).unsafeRunSync()

  def updateConfiguration(): Unit = {
    System.setProperty("db.database-url", s"postgres://tictactoe_user:tictactoe@localhost:$dbPort/tictactoe")
    System.setProperty("mail.server.smtp.port", mailSmtpPort.toString)
    ConfigFactory.invalidateCaches()
  }

  def repeatUntil[R](times: Int = 9, failureMsg: String = s"Condition was not fulfilled although attempted N times.")(
      predicate: => Option[R]
  ): R = {
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

  abstract override def run(testName: Option[String], args: Args): Status = withContainers {
    updateConfiguration()
    val cancel = EntryPoint.run(Nil).unsafeRunCancelable(System.err.println)
    waitForServer()
    val status = super.run(testName, args)
    cancel.unsafeRunSync()
    status
  }

}
