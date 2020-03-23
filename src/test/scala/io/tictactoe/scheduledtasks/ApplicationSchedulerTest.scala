package io.tictactoe.scheduledtasks

import io.tictactoe.scheduledtasks.tasks.SendMissingMails
import io.tictactoe.testutils.{Fixture, TestAppData}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.generators.Generators
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import cats.implicits._

class ApplicationSchedulerTest extends FlatSpec with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "send all missing emails" in new Fixture {

    forAll(Generators.missingEmails()) { emails =>
      val inputData = TestAppData(
        missingEmails = emails
      )

      val outputData = new SendMissingMails[TestAppState].executedTask.runS(inputData).unsafeRunSync()

      outputData.infoMessages contains allOf(
        "Checking for unsent emails.",
        s"Sending missing registration emails for ${emails.size} users.",
        emails.map(e => show"Mail sent to ${e.recipients.mkString_(", ")}."): _*
      )

      outputData.sentEmails should have size emails.size.toLong

      outputData.missingEmails shouldBe empty

    }

  }
}
