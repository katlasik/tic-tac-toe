package io.tictactoe.events

import java.util.UUID

import cats.data.NonEmptyList
import cats.implicits._
import io.tictactoe.emails.model._
import io.tictactoe.emails.values.{EmailMessageText, EmailMessageTitle}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.generators.Generators
import io.tictactoe.testutils.{Fixture, TestAppData}
import io.tictactoe.values.Email
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class EventBusTest extends FlatSpec with TableDrivenPropertyChecks with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "send registration confirmation emails, when new user is registered" in new Fixture {

    val inputData = TestAppData(
      uuids = List(
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        UUID.fromString("00000000-0000-0000-0000-000000000003")
      )
    )

    forAll(Generators.userRegisteredEvent()) { event =>
      val outputData = ApplicationEventHandler.live[TestAppState].handle(event).runS(inputData).unsafeRunSync()

      outputData.infoMessages should contain(show"Sending registration confirmation email to ${event.email}.")

      val email = EmailMessage(
        NonEmptyList.one(event.email),
        Email("no-reply@tictactoe.pl"),
        EmailMessageText(
          show"""Thanks for registering, ${event.username}!
                |
                |To confirm your account click on link below:
                |http://localhost:8082/registration?token=${event.confirmationToken}&id=${event.userId}""".stripMargin
        ),
        EmailMessageTitle(show"Hello, ${event.username}")
      )

      outputData.sentEmails should contain(email)

      outputData.savedEmails should contain(email)

      outputData.missingEmails shouldBe empty

    }

  }

}
