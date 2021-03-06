package io.tictactoe.specs

import io.circe.generic.auto._
import io.tictactoe.modules.authentication.model.AuthResponse
import io.tictactoe.testutils.ItTest
import io.tictactoe.modules.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.values.{AuthToken, UserId}
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen, Matchers}
import io.tictactoe.implicits._

class UserEndpointsItSpec extends FeatureSpec with GivenWhenThen with Matchers with ItTest with BeforeAndAfter {

  feature("User endpoints") {

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
        baseUrl("users/00000000-0000-0000-0000-000000000001"),
        Map("Authorization" -> s"Bearer $token")
      )

      result.success.json[DetailedUser].id shouldEq UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")

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

      result.success
        .json[List[SimpleUser]]
        .map(_.id.value.toString) should contain allOf ("00000000-0000-0000-0000-000000000001", "348d4be8-8b23-49f6-a1d7-227064bd8a23")

    }
  }

}
