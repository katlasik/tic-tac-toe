package io.tictactoe.events

import cats.data.NonEmptyList
import cats.implicits._
import io.tictactoe.emails.EmailMessage
import io.tictactoe.emails.services.values.{EmailMessageText, EmailMessageTitle}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.generators.Generators
import io.tictactoe.testutils.{Fixture, TestAppData}
import io.tictactoe.values.Email
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class EventBusTest extends FlatSpec with TableDrivenPropertyChecks with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "send registration confirmation emails, when new user is registered" in new Fixture {

    val inputData = TestAppData()

    forAll(Generators.userRegisteredEvent()) { event =>
      val outputData = ApplicationEventHandler.live[TestAppState].handle(event).runS(inputData).unsafeRunSync()

      outputData.infoMessages should contain(show"Sending registration confirmation email to ${event.email}.")

      outputData.emails should contain(
        EmailMessage(
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
      )

    }

  }

}
