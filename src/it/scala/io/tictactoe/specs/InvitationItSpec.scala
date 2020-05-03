package io.tictactoe.specs

import io.circe.generic.auto._
import io.tictactoe.authentication.model.AuthResponse
import io.tictactoe.testutils.ItTest
import io.tictactoe.values.AuthToken
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen, Matchers}

class InvitationItSpec extends FeatureSpec with GivenWhenThen with Matchers with ItTest with BeforeAndAfter {

  feature("Inviting users") {

    scenario("The user invites another user") {

      Given("a logged in user and password")
      sql("invitations.sql")

      val loginPayload =
        """{
          |"email": "user200@email.com",
          |"password": "razdwa"
          |}""".stripMargin

      val AuthToken(token) = post(baseUrl("login"), loginPayload).success.json[AuthResponse].token

      When("request for invitation of another user is sent")

      val payload =
        """{
          |"userId": "063be141-817d-4363-b59d-271b8392df1f"
          |}""".stripMargin

      post(baseUrl("games"), payload, Map("Authorization" -> s"Bearer $token")).success.plain

      Then("a guest receives email with invitation")

      getFirstMailContaining("You have been invited to play game of tic tac toe by user200.").recipient shouldBe "user201@email.com"

    }

    scenario("The user invites another person by email") {

      Given("a logged in user and password")
      sql("invitations.sql")

      val loginPayload =
        """{
          |"email": "user200@email.com",
          |"password": "razdwa"
          |}""".stripMargin

      val AuthToken(token) = post(baseUrl("login"), loginPayload).success.json[AuthResponse].token

      When("request for invitation of another user is sent")

      val payload =
        """{
          |"email": "guest@email.com"
          |}""".stripMargin

      post(baseUrl("games/invitation"), payload, Map("Authorization" -> s"Bearer $token")).success.plain

      Then("a guest receives email with invitation")

      getFirstMailContaining("You have been invited to play game of tic tac toe by user200.").recipient shouldBe "guest@email.com"

    }

  }

}
