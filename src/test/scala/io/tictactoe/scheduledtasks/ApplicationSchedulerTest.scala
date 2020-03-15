package io.tictactoe.scheduledtasks

import io.tictactoe.scheduledtasks.tasks.SendMissingRegistrationMails
import io.tictactoe.testutils.{Fixture, TestAppData}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.generators.Generators
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import cats.implicits._
import io.tictactoe.authentication.values.ConfirmationToken

class ApplicationSchedulerTest extends FlatSpec with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "disallow getting all players if user is unauthenticated" in new Fixture {

    val threeUsers = for {
      user1 <- Generators.user()
      user2 <- Generators.user()
      user3 <- Generators.user()
    } yield (user1, user2, user3)

    forAll(threeUsers) {
      case (user1, user2, user3) =>
        val inputData = TestAppData(
          users = List(user1, user2, user3),
          confirmationEmails = List((user1.id, user1.confirmationToken.get), (user2.id, ConfirmationToken("1")))
        )

        val outputData = new SendMissingRegistrationMails[TestAppState].executedTask.runS(inputData).unsafeRunSync()

        outputData.infoMessages contains allOf(
          show"Sending registration confirmation email to ${user2.email}.",
          show"Sending registration confirmation email to ${user3.email}.",
          "Sending missing registration emails for 2 users.",
          "Checking for unsent registration emails."
        )

        outputData.emails should have size 2

        outputData.confirmationEmails should have size 4

    }

  }
}
