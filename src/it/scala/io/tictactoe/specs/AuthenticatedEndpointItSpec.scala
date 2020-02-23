package io.tictactoe.specs

import io.circe.generic.auto._
import io.tictactoe.authentication.model.{AuthResponse, AuthToken}
import io.tictactoe.testutils.ItTest
import io.tictactoe.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.values.UserId
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen, Matchers}

class AuthenticatedEndpointItSpec extends FeatureSpec with GivenWhenThen with Matchers with ItTest with BeforeAndAfter {

  feature("Authenticated endpoint") {

    scenario("The user gets own account's details") {

      Given("there is user in database")

      sql("users.sql")

      And("the user logs in")

      val loginPayload =
        """{
           |"email": "user@email.com",
           |"password": "razdwa"
           |}""".stripMargin

      val AuthToken(token) = post(baseUrl("login"), loginPayload).success.json[AuthResponse].token

      Then("user should be able to get it's own account details")

      val result = get(
        baseUrl("users/7e9f3585-c1e3-4a71-b724-f5fdac912d32"),
        Map("Authorization" -> s"Bearer $token")
      )

      result.success.json[DetailedUser].id shouldBe UserId.fromString("7e9f3585-c1e3-4a71-b724-f5fdac912d32")

    }

    scenario("The user get users list") {

      Given("there are users in database")

      sql("users.sql")

      And("the user logs in")

      val loginPayload =
        """{
          |"email": "user@email.com",
          |"password": "razdwa"
          |}""".stripMargin

      val AuthToken(token) = post(baseUrl("login"), loginPayload).success.json[AuthResponse].token

      Then("user should be able to get it's own account details")

      val result = get(
        baseUrl("users"),
        Map("Authorization" -> s"Bearer $token")
      )

      result.success.json[List[SimpleUser]] should have size 2

    }
  }

}
