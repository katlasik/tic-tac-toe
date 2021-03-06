package io.tictactoe.routes

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import cats.data.NonEmptyList
import io.tictactoe.modules.authentication.model.{RawRegistrationRequest, RegistrationResult, User}
import io.tictactoe.testutils.{EqMatcher, Fixture, TestAppData}
import io.tictactoe.testutils.TestAppData.TestAppState
import org.http4s.Request
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.tictactoe.testutils.generators.Generators
import io.tictactoe.values.{Confirmed, Email, EventId, EventTimestamp, GameId, Hash, Unconfirmed, UserId, Username}
import org.http4s.implicits._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import cats.implicits._
import io.tictactoe.implicits._
import io.tictactoe.errors.ErrorView
import io.tictactoe.events.model.authentication.UserRegisteredEvent
import io.tictactoe.events.model.game.GameInvitationAccepted
import io.tictactoe.modules.game.model.GameInvitation
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.utilities.emails.model.EmailMessage
import io.tictactoe.utilities.emails.values.{EmailMessageText, EmailMessageTitle}
import org.scalacheck.Gen

class RegistrationTest extends FlatSpec with TableDrivenPropertyChecks with ScalaCheckDrivenPropertyChecks with Matchers with EqMatcher {

  it should "allow registering new players" in new Fixture {

    forAll(Generators.user()) { user =>

      val eventId = UUID.fromString("0000000-0000-0000-0000-000000000001")
      val timestamp = Instant.parse("2020-03-01T14:42:13.775935Z")

      val inputData = TestAppData(
        uuids = List(user.id.value, eventId),
        instants = List(timestamp),
        tokens = List(user.registrationConfirmationToken.get)
      )

      val request = Request[TestAppState](
        method = POST,
        uri = uri"registration"
      ).withEntity(RawRegistrationRequest(user.username.value, user.hash.value, user.email.value, none, none))

      val (outputData, Some(response)) = authModule.router.routes
        .run(request)
        .value
        .run(inputData)
        .unsafeRunSync()

      outputData.users should contain(user)

      outputData.infoMessages should contain(
        show"New user with id = ${user.id} was created."
      )

      outputData.events should contain(
        UserRegisteredEvent(
          EventId(eventId),
          EventTimestamp(timestamp),
          user.id,
          user.username,
          user.email,
          user.registrationConfirmationToken,
          Unconfirmed
        )
      )

      response.status.code shouldEq 200

      response.as[RegistrationResult].runA(inputData).unsafeRunSync() shouldEq RegistrationResult(user.id)
    }

  }

  it should "allow registering new players from invitation" in new Fixture {

    val gen: Gen[(User, ConfirmationToken, GameId)] =  for {
      user <- Generators.user(true)
      token <- Generators.confirmationToken()
      gameId <- Generators.id[GameId]
    } yield (user, token, gameId)

    forAll(gen) { case (user, token, gameId) =>
      val userRegisteredEventId = UUID.fromString("0000000-0000-0000-0000-000000000001")
      val gameAcceptedEventId = UUID.fromString("0000000-0000-0000-0000-000000000002")
      val timestamp = Instant.parse("2020-03-01T14:42:13.775935Z")
      val ownerId =  UserId.unsafeFromString("0000000-0000-0000-0000-000000000010")

      val inputData = TestAppData(
        uuids = List(user.id.value, gameAcceptedEventId, userRegisteredEventId),
        instants = List(timestamp, timestamp),
        dates = List(LocalDateTime.parse("2019-09-09T10:33:33")),
        users = List(
          User(
            ownerId,
            Username("owner"),
            Hash("userpass"),
            Email("user@email.com"),
            Confirmed,
            none,
            none
          )
        ),
        invitations = List(
          GameInvitation.withEmail(
            gameId,
            ownerId,
            user.email,
            token
          )
        )
      )

      val request = Request[TestAppState](
        method = POST,
        uri = uri"registration"
      ).withEntity(RawRegistrationRequest(user.username.value, user.hash.value, user.email.value, token.some, gameId.some))

      val (outputData, Some(response)) = authModule.router.routes
        .run(request)
        .value
        .run(inputData)
        .unsafeRunSync()

      outputData.users should contain(user)

      outputData.infoMessages should contain(
        show"New user with id = ${user.id} was created."
      )

      outputData.events should contain allOf(
        UserRegisteredEvent(
          EventId(userRegisteredEventId),
          EventTimestamp(timestamp),
          user.id,
          user.username,
          user.email,
          none,
          Confirmed
        ),
        GameInvitationAccepted(
          EventId(gameAcceptedEventId),
          EventTimestamp(timestamp),
          gameId,
          ownerId,
          user.id
        )
      )

      response.status.code shouldEq 200

      response.as[RegistrationResult].runA(inputData).unsafeRunSync() shouldEq RegistrationResult(user.id)
    }

  }


  it should "register new players from invitation as unconfirmed if token or gameId is incorrect" in new Fixture {

    val gen: Gen[(User, ConfirmationToken, GameId)] = for {
      user <- Generators.user()
      token <- Generators.confirmationToken()
      gameId <- Generators.id[GameId]
    } yield (user, token, gameId)

    forAll(gen) { case (user, token, gameId) =>
      val eventId = UUID.fromString("0000000-0000-0000-0000-000000000001")
      val timestamp = Instant.parse("2020-03-01T14:42:13.775935Z")
      val ownerId =  UserId.unsafeFromString("0000000-0000-0000-0000-000000000010")

      val inputData = TestAppData(
        uuids = List(user.id.value, eventId),
        tokens = List(user.registrationConfirmationToken.get),
        dates = List(LocalDateTime.parse("2019-09-09T10:33:33")),
        instants = List(timestamp),
        users = List(
          User(
            ownerId,
            Username("owner"),
            Hash("userpass"),
            Email("user@email.com"),
            Confirmed,
            none,
            none
          )
        ),
        invitations = List(
          GameInvitation.withEmail(
            gameId,
            ownerId,
            user.email,
            token
          )
        )
      )

      val request = Request[TestAppState](
        method = POST,
        uri = uri"registration"
      ).withEntity(RawRegistrationRequest(user.username.value, user.hash.value, user.email.value, ConfirmationToken("illegal").some, gameId.some))

      val (outputData, Some(response)) = authModule.router.routes
        .run(request)
        .value
        .run(inputData)
        .unsafeRunSync()

      outputData.users should contain(user)

      outputData.infoMessages should contain(
        show"New user with id = ${user.id} was created."
      )

      outputData.events should contain(
        UserRegisteredEvent(
          EventId(eventId),
          EventTimestamp(timestamp),
          user.id,
          user.username,
          user.email,
          user.registrationConfirmationToken,
          Unconfirmed
        )
      )

      response.status.code shouldEq 200

      response.as[RegistrationResult].runA(inputData).unsafeRunSync() shouldEq RegistrationResult(user.id)
    }

  }

  it should "disallow registering new player if there are any errors" in new Fixture {

    val inputData = TestAppData(
      users = List(
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
          Username("user1"),
          Hash("userpass"),
          Email("email@user.pl"),
          Unconfirmed,
          None,
          None
        )
      )
    )

    val cases = Table(
      ("request", "error message"),
      (RawRegistrationRequest("user2", "userpass", "email@user.pl", none, none), "Email already exists."),
      (RawRegistrationRequest("user1", "userpass", "email1@user.pl", none, none), "Username already exists."),
      (RawRegistrationRequest("u", "userpass", "email1@user.pl", none, none), "Username length must be at least 2."),
      (RawRegistrationRequest("**user", "userpass", "email1@user.pl", none, none), "Username has illegal characters."),
      (RawRegistrationRequest("user", "userpass", "email1@", none, none), "Email has wrong format."),
      (RawRegistrationRequest("user", "u", "email1@user.pl", none, none), "Password needs to have at least 6 characters."),
      (
        RawRegistrationRequest(
          "user",
          "userpass",
          "email1@user.pl",
          none,
          GameId.unsafeFromString("00000000-0000-0000-0000-000000000001").some
        ),
        "Both invitationToken and gameId have to be provided or neither."
      ),
      (
        RawRegistrationRequest(
          "user",
          "userpass",
          "email1@user.pl",
          ConfirmationToken("token").some,
          none
        ),
        "Both invitationToken and gameId have to be provided or neither."
      )
    )

    forAll(cases) { (requestEntity: RawRegistrationRequest, errorMessage: String) =>
      val request = Request[TestAppState](
        method = POST,
        uri = uri"registration"
      ).withEntity(requestEntity)

      val (outputData, Some(response)) = authModule.router.routes
        .run(request)
        .value
        .run(inputData)
        .unsafeRunSync()

      outputData.users should have size 1

      outputData.infoMessages shouldBe empty

      response.status.code shouldEq 400

      response.as[ErrorView].runA(inputData).unsafeRunSync() shouldEq ErrorView(errorMessage)

    }

  }

  it should "allow confirming users" in new Fixture {

    val inputData = TestAppData(
      users = List(
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Unconfirmed,
          ConfirmationToken("1").some,
          None
        )
      )
    )

    val request = Request[TestAppState](
      method = POST,
      uri = uri"registration/confirmation?token=1&id=00000000-0000-0000-0000-000000000001"
    )

    val (data, Some(response)) = authModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 200

    data.users should contain(
      User(
        UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
        Username("user"),
        Hash("userpass"),
        Email("email@user.pl"),
        Confirmed,
        None,
        None
      )
    )

  }

  it should "allow sending requests for resending confirmation emails" in new Fixture {

    val newToken = ConfirmationToken("2")
    val userId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
    val username = Username("user")
    val hash = Hash("userpass")
    val email = Email("email@user.pl")

    val inputData = TestAppData(
      uuids = List(
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        UUID.fromString("00000000-0000-0000-0000-000000000003")
      ),
      tokens = List(newToken),
      users = List(
        User(
          userId,
          username,
          hash,
          email,
          Unconfirmed,
          ConfirmationToken("1").some,
          None
        )
      )
    )

    val request = Request[TestAppState](
      method = PUT,
      uri = uri"registration?email=email@user.pl"
    )

    val (data, Some(response)) = authModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 200

    data.infoMessages should contain allOf (
      "Sending of new registration confirmation email requested by email@user.pl.",
      "Sending registration confirmation email to email@user.pl."
    )

    data.sentEmails should contain(
      EmailMessage(
        NonEmptyList.one(email),
        Email("no-reply@tictactoe.io"),
        EmailMessageText(
          show"""Thanks for registering, $username!
                |
                |To confirm your account click on link below:
                |http://localhost:8082/registration/confirmation?token=$newToken&id=$userId""".stripMargin
        ),
        EmailMessageTitle(show"Hello, $username!")
      )
    )

    data.missingEmails shouldBe empty

  }

  it should "allow reject requests for resending confirmation emails if user is already confirmed" in new Fixture {

    val newToken = ConfirmationToken("2")
    val id = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
    val username = Username("user")
    val hash = Hash("userpass")
    val email = Email("email@user.pl")

    val inputData = TestAppData(
      tokens = List(newToken),
      users = List(
        User(
          id,
          username,
          hash,
          email,
          Confirmed,
          None,
          None
        )
      )
    )

    val request = Request[TestAppState](
      method = PUT,
      uri = uri"registration?email=email@user.pl"
    )

    val (data, Some(response)) = authModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 404

    data.sentEmails shouldBe empty

    data.missingEmails shouldBe empty

  }

}
