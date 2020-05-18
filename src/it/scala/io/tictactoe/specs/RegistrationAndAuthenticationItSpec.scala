package io.tictactoe.specs

import io.tictactoe.modules.authentication.model.AuthResponse
import io.tictactoe.testutils.{CapturedMail, ItTest}
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen, Matchers}
import io.circe.generic.auto._
import io.tictactoe.implicits._

class RegistrationAndAuthenticationItSpec extends FeatureSpec with GivenWhenThen with Matchers with ItTest with BeforeAndAfter {

  feature("Registration and authentication") {

    scenario("The user registers and logs in") {

      val UrlRegex = "https?:.+".r

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
      registrationResponse.status shouldEq 200

      And("the registration confirmation email is sent")

      val CapturedMail(confirmationUrl, _) = getFirstMailMatching(UrlRegex.findFirstIn(_))

      When("the user uses confirmation link")
      post(confirmationUrl)

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
