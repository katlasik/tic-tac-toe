package io.tictactoe.routes

import java.time.Instant
import java.util.UUID

import cats.data.NonEmptyList
import io.tictactoe.authentication.model.{RegistrationRequest, RegistrationResult, User}
import io.tictactoe.testutils.{Fixture, TestAppData}
import io.tictactoe.testutils.TestAppData.TestAppState
import org.http4s.Request
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.tictactoe.authentication.services.Hash
import io.tictactoe.error.ErrorView
import io.tictactoe.testutils.generators.Generators
import io.tictactoe.values.{Email, EventId, EventTimestamp, Unconfirmed, UserId, Username, Confirmed}
import org.http4s.implicits._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import cats.implicits._
import io.tictactoe.authentication.events.UserRegisteredEvent
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.infrastructure.emails.model.EmailMessage
import io.tictactoe.infrastructure.emails.values.{EmailMessageText, EmailMessageTitle}

class RegistrationTest extends FlatSpec with TableDrivenPropertyChecks with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "allow registering new players" in new Fixture {

    import dsl._

    forAll(Generators.user()) { user =>
      val eventId = UUID.fromString("0000000-0000-0000-0000-000000000001")
      val timestamp = Instant.parse("2020-03-01T14:42:13.775935Z")

      val inputData = TestAppData(
        uuids = List(user.id.value, eventId),
        dates = List(timestamp),
        tokens = List(user.registrationConfirmationToken.get)
      )

      val request = Request[TestAppState](
        method = POST,
        uri = uri"registration"
      ).withEntity(RegistrationRequest(user.username.value, user.hash.value, user.email.value))

      val (outputData, Some(response)) = PublicRouter
        .routes[TestAppState]
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
          user.registrationConfirmationToken.get
        )
      )

      response.status.code shouldBe 200

      response.as[RegistrationResult].runA(inputData).unsafeRunSync() shouldBe RegistrationResult(user.id)
    }

  }

  it should "disallow registering new player if there are any errors" in new Fixture {

    import dsl._

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
      (RegistrationRequest("user2", "userpass", "email@user.pl"), "Email already exists."),
      (RegistrationRequest("user1", "userpass", "email1@user.pl"), "Username already exists."),
      (RegistrationRequest("u", "userpass", "email1@user.pl"), "Username length must be at least 2."),
      (RegistrationRequest("**user", "userpass", "email1@user.pl"), "Username has illegal characters."),
      (RegistrationRequest("user", "userpass", "email1"), "Email has wrong format."),
      (RegistrationRequest("user", "u", "email1@user.pl"), "Password needs to have at least 6 characters.")
    )

    forAll(cases) { (requestEntity: RegistrationRequest, errorMessage: String) =>
      val request = Request[TestAppState](
        method = POST,
        uri = uri"registration"
      ).withEntity(requestEntity)

      val (outputData, Some(response)) = PublicRouter
        .routes[TestAppState]
        .run(request)
        .value
        .run(inputData)
        .unsafeRunSync()

      outputData.users should have size 1

      outputData.infoMessages shouldBe empty

      response.status.code shouldBe 400

      response.as[ErrorView].runA(inputData).unsafeRunSync() shouldBe ErrorView(errorMessage)

    }

  }

  it should "allow confirming users" in new Fixture {

    import dsl._

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
      method = GET,
      uri = uri"registration?token=1&id=00000000-0000-0000-0000-000000000001"
    )

    val (data, Some(response)) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 303

    response.headers.get("Location".ci).get.value shouldBe "http://localhost:8082"

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

    import dsl._

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

    val (data, Some(response)) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 200

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
                |http://localhost:8082/registration?token=$newToken&id=$userId""".stripMargin
        ),
        EmailMessageTitle(show"Hello, $username!")
      )
    )

    data.missingEmails shouldBe empty

  }

  it should "allow reject requests for resending confirmation emails if user is already confirmed" in new Fixture {

    import dsl._

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

    val (data, Some(response)) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 404

    data.sentEmails shouldBe empty

    data.missingEmails shouldBe empty

  }

}
