package io.tictactoe.events

import java.util.UUID

import cats.data.NonEmptyList
import cats.implicits._
import io.tictactoe.modules.authentication.model.User
import io.tictactoe.events.model.authentication.PasswordChangedEvent
import io.tictactoe.modules.game.model.Game
import io.tictactoe.utilities.emails.model.EmailMessage
import io.tictactoe.utilities.emails.values.{EmailMessageText, EmailMessageTitle}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.generators.Generators
import io.tictactoe.testutils.{EqMatcher, Fixture, TestAppData}
import io.tictactoe.values.{Email, EventId, EventTimestamp, Hash, Unconfirmed, UserId, Username}
import org.scalacheck.Gen
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import io.tictactoe.implicits._

class ApplicationEventsHandlerTest extends FlatSpec with ScalaCheckDrivenPropertyChecks with Matchers with EqMatcher {

  it should "create new game in case invitation is accepted" in new Fixture {

    val gen = for {
      event <- Generators.gameInvitationAccepted()
      rand <- Gen.chooseNum(0, 1)
      initialPlayer = List(event.ownerId, event.guestId)(rand)
    } yield (event, rand, initialPlayer)

    val handler = ApplicationEventHandler.live[TestAppState](authModule, gameModule).runEmptyA.unsafeRunSync()

    forAll(gen) { case (event, rand, initialPlayer) =>

      val inputData = TestAppData(
        randomInts = List(rand)
      )

      val outputData = handler.handle(event).runS(inputData).unsafeRunSync()

      outputData.games should contain(
        Game(
          event.gameId,
          event.ownerId,
          event.guestId,
          initialPlayer
        )
      )

    }

  }

  it should "send registration confirmation emails, when new user is registered" in new Fixture {

    val inputData = TestAppData(
      uuids = List(
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        UUID.fromString("00000000-0000-0000-0000-000000000003")
      )
    )

    val handler = ApplicationEventHandler.live[TestAppState](authModule, gameModule).runEmptyA.unsafeRunSync()

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

    val handler = ApplicationEventHandler.live[TestAppState](authModule, gameModule).runEmptyA.unsafeRunSync()

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
