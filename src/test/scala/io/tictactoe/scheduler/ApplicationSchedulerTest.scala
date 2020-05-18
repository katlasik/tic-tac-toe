package io.tictactoe.scheduler

import io.tictactoe.scheduler.tasks.SendMissingMails
import io.tictactoe.testutils.{EqMatcher, Fixture, TestAppData}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.generators.Generators
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import cats.implicits._
import io.tictactoe.implicits._

class ApplicationSchedulerTest extends FlatSpec with ScalaCheckDrivenPropertyChecks with Matchers with EqMatcher {

  it should "send all missing emails" in new Fixture {

    forAll(Generators.missingEmails()) { emails =>
      val inputData = TestAppData(
        missingEmails = emails
      )

      val outputData = new SendMissingMails[TestAppState].executedTask.runS(inputData).unsafeRunSync()

      outputData.infoMessages contains allOf(
        "Running check for unsent emails.",
        s"Sending missing registration emails for ${emails.size} users.",
        emails.map(e => show"Mail sent to ${e.recipients.mkString_(", ")}."): _*
      )

      outputData.sentEmails should have size emails.size.toLong

      outputData.missingEmails shouldBe empty

    }

  }
}
