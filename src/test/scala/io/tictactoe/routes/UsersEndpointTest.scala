package io.tictactoe.routes

import io.tictactoe.modules.authentication.model.{Credentials, User}
import io.tictactoe.testutils.{EqMatcher, Fixture, TestAppData}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.modules.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.values.{Confirmed, Email, Hash, Password, UserId, Username}
import org.http4s.{Header, Headers, Request}
import org.scalatest.{FlatSpec, Matchers}
import org.http4s.implicits._
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._
import io.tictactoe.testutils.generators.Generators
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import henkan.convert.Syntax._
import io.tictactoe.errors.ErrorView
import cats.implicits._
import io.tictactoe.implicits._

class UsersEndpointTest extends FlatSpec with ScalaCheckDrivenPropertyChecks with Matchers with EqMatcher {

  it should "disallow getting all players if user is unauthenticated" in new Fixture {

    forAll(Generators.users(5, 10)) { allUsers =>
      val inputData = TestAppData(
        users = allUsers
      )

      val request = Request[TestAppState](
        method = GET,
        uri = uri"users",
        headers = Headers(List(Header("Authorization", s"Bearer illegal")))
      )

      val Some(response) = userModule.router.routes
        .run(request)
        .value
        .runA(inputData)
        .unsafeRunSync()

      response.status.code shouldEq 401

      response.as[ErrorView].runA(inputData).unsafeRunSync() shouldEq ErrorView("Could not verify signature")
    }

  }

  it should "allow getting all players if user is authenticated" in new Fixture {

    val userId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")

    val email = Email("email@gmail.com")

    val hash = Hash("password")

    val user = User(userId, Username("user"), hash, email, Confirmed, None, None)

    forAll(Generators.users()) { users =>
      val allUsers = user :: users

      val inputData = TestAppData(
        users = allUsers
      )

      val token = authenticate(Credentials(email, Password(hash.value))).runA(inputData).unsafeRunSync()

      val request = Request[TestAppState](
        method = GET,
        uri = uri"users",
        headers = Headers(List(Header("Authorization", s"Bearer $token")))
      )

      val Some(response) = userModule.router.routes
        .run(request)
        .value
        .runA(inputData)
        .unsafeRunSync()

      response.status.code shouldEq 200

      response.as[List[SimpleUser]].runA(inputData).unsafeRunSync() shouldEq allUsers.map(_.to[SimpleUser]())
    }

  }

  it should "allow getting player's own details" in new Fixture {

    val user =
      User(
        UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
        Username("user"),
        Hash("password"),
        Email("email@gmail.com"),
        Confirmed,
        None,
        None
      )

    val inputData = TestAppData(
      users = List(user)
    )

    val token = authenticate(Credentials(user.email, Password(user.hash.value))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = GET,
      uri = uri(show"users/${user.id}"),
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    )

    val Some(response) = userModule.router.routes
      .run(request)
      .value
      .runA(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 200

    response.as[DetailedUser].runA(inputData).unsafeRunSync() shouldEq user.to[DetailedUser]()

  }

  it should "forbid getting another user's details" in new Fixture {

    val user =
      User(
        UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
        Username("user"),
        Hash("password"),
        Email("email@gmail.com"),
        Confirmed,
        None,
        None
      )

    val anotherUser =
      User(
        UserId.unsafeFromString("00000000-0000-0000-0000-000000000002"),
        Username("user2"),
        Hash("password"),
        Email("email2@gmail.com"),
        Confirmed,
        None,
        None
      )

    val inputData = TestAppData(
      users = List(
        user,
        anotherUser
      )
    )

    val token = authenticate(Credentials(user.email, Password(user.hash.value))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = GET,
      uri = uri(show"users/${anotherUser.id}"),
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    )

    val Some(response) = userModule.router.routes
      .run(request)
      .value
      .runA(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 403

    response.as[ErrorView].runA(inputData).unsafeRunSync() shouldEq ErrorView("Access to resource is forbidden!")

  }

}
