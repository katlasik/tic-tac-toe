package io.tictactoe.routes

import java.time.Instant
import java.util.UUID

import cats.data.NonEmptyList
import io.tictactoe.authentication.model.{PasswordChangeRequest, User}
import io.tictactoe.testutils.{Fixture, TestAppData}
import io.tictactoe.testutils.TestAppData.TestAppState
import org.http4s.Request
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.tictactoe.authentication.services.Hash
import io.tictactoe.values.{Email, EventId, EventTimestamp, No, Password, UserId, Username, Yes}
import org.http4s.implicits._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import cats.implicits._
import io.tictactoe.authentication.events.PasswordChangedEvent
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.infrastructure.emails.model.EmailMessage
import io.tictactoe.infrastructure.emails.values.{EmailMessageText, EmailMessageTitle}

class ResetPasswordTest  extends FlatSpec with TableDrivenPropertyChecks with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "allow sending requests to reset password" in new Fixture {

    import dsl._

    val newToken = ConfirmationToken("2")
    val id = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
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
          id,
          username,
          hash,
          email,
          Yes,
          None,
          None
        )
      )
    )

    val request = Request[TestAppState](
      method = POST,
      uri = uri"password?email=email@user.pl"
    )

    val (data, Some(response)) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 200

    val message = EmailMessage(
      NonEmptyList.one(email),
      Email("no-reply@tictactoe.pl"),
      EmailMessageText(
        show"""You have requested resetting your password.
              |
              |To reset your password visit link below:
              |http://localhost:8082/newpassword?token=2&id=$id""".stripMargin
      ),
      EmailMessageTitle(show"Hello, $username!")
    )

    data.infoMessages should contain allOf (
      "Sending password change request email to email@user.pl.",
      "Sending of password reset mail requested for user with id = 00000000-0000-0000-0000-000000000001 and email = email@user.pl."
    )

    data.sentEmails should contain(message)

    data.missingEmails shouldBe empty

  }

  it should "ignore sending requests to reset password if user is not confirmed" in new Fixture {

    import dsl._

    val id = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
    val username = Username("user")
    val hash = Hash("userpass")
    val email = Email("email@user.pl")

    val inputData = TestAppData(
      users = List(
        User(
          id,
          username,
          hash,
          email,
          No,
          None,
          None
        )
      )
    )

    val request = Request[TestAppState](
      method = POST,
      uri = uri"password?email=email@user.pl"
    )

    val (data, Some(response)) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 200

    data.sentEmails shouldBe empty

    data.missingEmails shouldBe empty

    data.infoMessages should contain("No confirmed user with mail email@user.pl found in database, sending no email.")

  }

  it should "ignore sending requests to reset password if user doesn't exist" in new Fixture {

    import dsl._

    val inputData = TestAppData()

    val request = Request[TestAppState](
      method = POST,
      uri = uri"password?email=wrong@user.pl"
    )

    val (data, Some(response)) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 200

    data.sentEmails shouldBe empty

    data.missingEmails shouldBe empty

    data.infoMessages should contain("No confirmed user with mail wrong@user.pl found in database, sending no email.")

  }

  it should "allow changing password if token is correct" in new Fixture {

    import dsl._

    val userId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
    val username = Username("user")
    val hash = Hash("userpass")
    val email = Email("email@user.pl")
    val token = ConfirmationToken("1")
    val eventId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    val eventTimestamp =
      Instant.parse("2020-03-01T14:42:13.775935Z")

    val inputData = TestAppData(
      uuids = List(eventId),
      dates = List(eventTimestamp),
      users = List(
        User(
          userId,
          username,
          hash,
          email,
          No,
          None,
          token.some
        )
      )
    )

    val request = Request[TestAppState](
      method = POST,
      uri = uri"password/change"
    ).withEntity(
      PasswordChangeRequest(
        userId,
        token,
        Password("newpass")
      )
    )

    val (data, Some(response)) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 200

    data.users.head.hash shouldBe Hash("newpass")

    data.infoMessages should contain(show"Password of user with id $userId was changed.")

    data.events should contain(PasswordChangedEvent(EventId(eventId), EventTimestamp(eventTimestamp), userId))

  }

  it should "reject changing password if token is incorrect" in new Fixture {

    import dsl._

    val userId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
    val username = Username("user")
    val hash = Hash("userpass")
    val email = Email("email@user.pl")

    val inputData = TestAppData(
      users = List(
        User(
          userId,
          username,
          hash,
          email,
          No,
          None,
          ConfirmationToken("1").some
        )
      )
    )

    val request = Request[TestAppState](
      method = POST,
      uri = uri"password/change"
    ).withEntity(
      PasswordChangeRequest(
        userId,
        ConfirmationToken("2"),
        Password("newpass")
      )
    )

    val (data, Some(response)) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 400

    data.users.head.hash shouldBe Hash("userpass")

    data.infoMessages shouldBe empty

    data.events shouldBe empty

  }

  it should "reject changing password if user doesn't exist" in new Fixture {

    import dsl._

    val inputData = TestAppData()

    val request = Request[TestAppState](
      method = POST,
      uri = uri"password/change"
    ).withEntity(
      PasswordChangeRequest(
        UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
        ConfirmationToken("1"),
        Password("newpass")
      )
    )

    val (data, Some(response)) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 400

    data.infoMessages shouldBe empty

    data.events shouldBe empty

  }

}
