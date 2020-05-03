package io.tictactoe.events

import java.util.UUID

import cats.data.NonEmptyList
import cats.implicits._
import io.tictactoe.authentication.infrastructure.effects.Hash
import io.tictactoe.authentication.model.User
import io.tictactoe.events.model.authentication.PasswordChangedEvent
import io.tictactoe.utilities.emails.model.EmailMessage
import io.tictactoe.utilities.emails.values.{EmailMessageText, EmailMessageTitle}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.generators.Generators
import io.tictactoe.testutils.{Fixture, TestAppData}
import io.tictactoe.values.{Email, EventId, EventTimestamp, Unconfirmed, UserId, Username}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class ApplicationEventsHandlerTest extends FlatSpec with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "send registration confirmation emails, when new user is registered" in new Fixture {

    val inputData = TestAppData(
      uuids = List(
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        UUID.fromString("00000000-0000-0000-0000-000000000003")
      )
    )

    val handler = ApplicationEventHandler.live[TestAppState](authModule).runEmptyA.unsafeRunSync()

    forAll(Generators.userRegisteredEvent()) { event =>
      val outputData = handler.handle(event).runS(inputData).unsafeRunSync()

      outputData.infoMessages should contain(show"Sending registration confirmation email to ${event.email}.")

      val email = EmailMessage(
        NonEmptyList.one(event.email),
        Email("no-reply@tictactoe.io"),
        EmailMessageText(
          show"""Thanks for registering, ${event.username}!
                |
                |To confirm your account click on link below:
                |http://localhost:8082/registration/confirmation?token=${event.confirmationToken.get}&id=${event.userId}""".stripMargin
        ),
        EmailMessageTitle(show"Hello, ${event.username}!")
      )

      outputData.sentEmails should contain(email)

      outputData.savedEmails should contain(email)

      outputData.missingEmails shouldBe empty

    }

  }

  it should "send password changed confirmation emails, when password of user is changed" in new Fixture {

    val userId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
    val email = Email("email@user.pl")
    val username = Username("user1")

    val inputData = TestAppData(
      uuids = List(UUID.fromString("00000000-0000-0000-0000-000000000002")),
      users = List(
        User(
          userId,
          username,
          Hash("userpass"),
          email,
          Unconfirmed,
          None,
          None
        )
      )
    )

    val event = PasswordChangedEvent(
      EventId.unsafeFromString("0000000-0000-0000-0000-000000000001"),
      EventTimestamp.unsafeFromString("2020-03-01T14:42:13.775935Z"),
      userId,
      username,
      email
    )

    val handler = ApplicationEventHandler.live[TestAppState](authModule).runEmptyA.unsafeRunSync()

    val outputData = handler.handle(event).runS(inputData).unsafeRunSync()

    val emailMessage = EmailMessage(
      NonEmptyList.one(email),
      Email("no-reply@tictactoe.io"),
      EmailMessageText("Your password has been successfully changed."),
      EmailMessageTitle(show"Hello, user1!")
    )


    outputData.sentEmails should contain(emailMessage)

    outputData.savedEmails should contain(emailMessage)

    outputData.missingEmails shouldBe empty

  }

}
