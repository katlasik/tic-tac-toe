package io.tictactoe.routes

import io.tictactoe.modules.authentication.model.{AuthResponse, Credentials, User}
import io.tictactoe.testutils.{Fixture, TestAppData}
import io.tictactoe.testutils.TestAppData.TestAppState
import org.http4s.Request
import org.scalatest.{FlatSpec, Matchers}
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.tictactoe.modules.authentication.infrastructure.effects.Hash
import io.tictactoe.errors.ErrorView
import io.tictactoe.values.{AuthToken, Confirmed, Email, Password, Unconfirmed, UserId, Username}
import org.http4s.implicits._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class LoginTest extends FlatSpec with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "allow login in as user" in new Fixture {

    val inputData = TestAppData(
      users = List(
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
    )

    val request = Request[TestAppState](
      method = POST,
      uri = uri"login"
    ).withEntity(Credentials(Email("email@user.pl"), Password("userpass")))

    val (outputData, Some(response)) = authModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    outputData.infoMessages should contain("User with id = 00000000-0000-0000-0000-000000000001 authenticated.")

    response.status.code shouldBe 200

    val Some(token) = response.headers.get("Set-Auth-Token".ci).map(_.value)

    response.as[AuthResponse].runA(inputData).unsafeRunSync() shouldBe AuthResponse(AuthToken(token))

  }

  it should "reject login if user is unconfirmed" in new Fixture {

    val inputData = TestAppData(
      users = List(
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Unconfirmed,
          None,
          None
        )
      )
    )

    val request = Request[TestAppState](
      method = POST,
      uri = uri"login"
    ).withEntity(Credentials(Email("email@user.pl"), Password("userpass")))

    val (outputData, Some(response)) = authModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    outputData.infoMessages shouldBe empty

    response.status.code shouldBe 401

    response.headers.get("Set-Auth-Token".ci) shouldBe None

    response.as[ErrorView].runA(inputData).unsafeRunSync() shouldBe ErrorView("Account is not yet confirmed.")

  }

  it should "reject unsuccessful login attempts" in new Fixture {

    val inputData = TestAppData(
      users = List(
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
    )

    val request = Request[TestAppState](
      method = POST,
      uri = uri"login"
    ).withEntity(Credentials(Email("email@user.pl"), Password("xxxxxx")))

    val Some(response) = authModule.router.routes
      .run(request)
      .value
      .runA(inputData)
      .unsafeRunSync()

    response.status.code shouldBe 401

    response.headers.get("Set-Auth-Token".ci) shouldBe None

    response.as[ErrorView].runA(inputData).unsafeRunSync() shouldBe ErrorView("Invalid credentials.")

  }

}
