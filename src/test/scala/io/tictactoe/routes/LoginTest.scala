package io.tictactoe.routes

import io.tictactoe.authentication.model.{AuthResponse, Credentials, User}
import io.tictactoe.testutils.{Fixture, TestAppData}
import io.tictactoe.testutils.TestAppData.TestAppState
import org.http4s.Request
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.tictactoe.authentication.services.Hash
import io.tictactoe.error.ErrorView
import io.tictactoe.values.{Email, No, Password, UserId, Username, Yes}
import org.http4s.implicits._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import io.tictactoe.authentication.values.AuthToken

class LoginTest extends FlatSpec with TableDrivenPropertyChecks with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "allow login in as user" in new Fixture {

    import dsl._

    val inputData = TestAppData(
      users = List(
        User(
          UserId.fromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Yes,
          None,
          None
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

  it should "reject login if user is unconfirmed" in new Fixture {

    import dsl._

    val inputData = TestAppData(
      users = List(
        User(
          UserId.fromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          No,
          None,
          None
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

    outputData.infoMessages shouldBe empty

    response.status.code shouldBe 401

    response.headers.get("Set-Auth-Token".ci) shouldBe None

    response.as[ErrorView].runA(inputData).unsafeRunSync() shouldBe ErrorView("Account is not yet confirmed.")

  }

  it should "reject unsuccessful login attempts" in new Fixture {

    import dsl._

    val inputData = TestAppData(
      users = List(
        User(
          UserId.fromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Yes,
          None,
          None
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
