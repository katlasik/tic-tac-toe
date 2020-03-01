package io.tictactoe.routes

import java.time.Instant
import java.util.UUID

import io.tictactoe.authentication.model.{AuthResponse, AuthToken, Credentials, RegistrationRequest, RegistrationResult, User}
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
import io.tictactoe.values.{Email, EventId, EventTimestamp, No, Password, UserId, Username}
import org.http4s.implicits._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import cats.implicits._
import io.tictactoe.authentication.events.UserRegisteredEvent

class PublicRouterTest extends FlatSpec with TableDrivenPropertyChecks with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "allow registering new players" in new Fixture {

    import dsl._

    forAll(Generators.user()) { user =>
      val eventId = UUID.fromString("0000000-0000-0000-0000-000000000001")
      val timestamp = Instant.parse("2020-03-01T14:42:13.775935Z")

      val inputData = TestAppData(
        uuids = List(user.id.value, eventId),
        dates = List(timestamp)
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

      outputData.events should contain(UserRegisteredEvent(EventId(eventId), EventTimestamp(timestamp), user.username, user.email))

      response.status.code shouldBe 200

      response.as[RegistrationResult].runA(inputData).unsafeRunSync() shouldBe RegistrationResult(user.id)
    }

  }

  it should "disallow registering new player if there are any errors" in new Fixture {

    import dsl._

    val inputData = TestAppData(
      users = List(
        User(
          UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
          Username("user1"),
          Hash("userpass"),
          Email("email@user.pl"),
          No
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

  it should "allow login in as user" in new Fixture {

    import dsl._

    val inputData = TestAppData(
      users = List(
        User(
          UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          No
        )
      )
    )

    val request = Request[TestAppState](
      method = POST,
      uri = uri"login"
    ).withEntity(Credentials(Email("email@user.pl"), Password("userpass")))

    val (outputData, Some(response)) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    outputData.infoMessages should contain("User with id = 00000000-0000-0000-0000-000000000001 authenticated.")

    response.status.code shouldBe 200

    val Some(token) = response.headers.get("Set-Auth-Token".ci).map(_.value)

    response.as[AuthResponse].runA(inputData).unsafeRunSync() shouldBe AuthResponse(AuthToken(token))

  }

  it should "reject unsuccessful login attempts" in new Fixture {

    import dsl._

    val inputData = TestAppData(
      users = List(
        User(
          UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          No
        )
      )
    )

    val request = Request[TestAppState](
      method = POST,
      uri = uri"login"
    ).withEntity(Credentials(Email("email@user.pl"), Password("xxxxxx")))

    val Some(response) = PublicRouter
      .routes[TestAppState]
      .run(request)
      .value
      .runA(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 401

    response.headers.get("Set-Auth-Token".ci) shouldBe None

    response.as[ErrorView].runA(inputData).unsafeRunSync() shouldBe ErrorView("Invalid credentials.")

  }

}
