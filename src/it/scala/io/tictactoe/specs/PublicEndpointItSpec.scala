package io.tictactoe.specs

import io.tictactoe.authentication.model.AuthResponse
import io.tictactoe.testutils.ItTest
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen, Matchers}
import io.circe.generic.auto._
import io.circe._
import io.circe.parser._

class PublicEndpointItSpec extends FeatureSpec with GivenWhenThen with Matchers with ItTest with BeforeAndAfter {

  feature("Public endpoint") {

    val UrlRegex = "https?:.+".r

    scenario("The user registers and logs in") {

      Given("a user and password")
      val user = "User0"
      val email = "user0@email.com"
      val password = "Test123456"

      When("request for registration is sent")
      val registrationPayload =
        s"""
           |{
           |  "username": "$user",
           |	"email": "$email",
           |	"password": "$password"
           |}
           |""".stripMargin

      val registrationResponse = post(baseUrl("registration"), registrationPayload)

      Then("the user is registered")
      registrationResponse.status shouldBe 200

      And("the registration confirmation email is sent")

      val confirmationUrl = repeatUntil() {
        val response = get(s"http://localhost:$mailRestPort/api/v2/messages").success.plain

        val body =
          parse(response).getOrElse(Json.Null).hcursor.downField("items").downArray.downField("Content").downField("Body").as[String]

        body.toOption.flatMap(UrlRegex.findFirstIn)
      }

      When("the user uses confirmation link")
      get(confirmationUrl)

      And("new user tries to log in ")
      val loginPayload =
        s"""{
          |"email": "$email",
          |"password": "$password"
          |}""".stripMargin
      val authenticationResponse = post(baseUrl("login"), loginPayload)

      Then("he is logged in")
      authenticationResponse.headers("Set-Auth-Token") should not be empty
      authenticationResponse.success.json[AuthResponse].token.value should not be empty

    }

  }

}
