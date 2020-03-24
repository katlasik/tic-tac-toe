package io.tictactoe.specs

import io.tictactoe.authentication.model.AuthResponse
import io.tictactoe.testutils.ItTest
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen, Matchers}
import io.circe.generic.auto._

class PublicEndpointItSpec extends FeatureSpec with GivenWhenThen with Matchers with ItTest with BeforeAndAfter {

  feature("Public endpoint") {

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
      registrationResponse.status shouldBe 200

      And("the registration confirmation email is sent")

      val confirmationUrl = UrlRegex.findFirstIn(getMails(1).head).get

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

    scenario("The user forgets password and want to change it") {

      val TokenRegex = "(?<=token=).*(?=&)".r
      val IdRegex = "(?<=id=).*".r

      Given("a user and password")
      sql("passwords.sql")
      val email = "user100@email.com"
      val newPassword = "Test999999"

      When("request for registration is sent")
      post(baseUrl(s"password?email=$email"))

      Then("the user receives token on mail")
      val mail = getMails(1).head
      val token = TokenRegex.findFirstIn(mail).get
      val id = IdRegex.findFirstIn(mail).get

      When("the user uses token to change password")
      val passwordChangePayload =
        s"""{
           |"id": "$id",
           |"token": "$token",
           |"password": "$newPassword"
           |}""".stripMargin

      post(baseUrl("password/change"), passwordChangePayload).success.plain

      And("the user tries to log in ")
      val loginPayload =
        s"""{
           |"email": "$email",
           |"password": "$newPassword"
           |}""".stripMargin
      val authenticationResponse = post(baseUrl("login"), loginPayload)

      Then("he is logged in")
      authenticationResponse.headers("Set-Auth-Token") should not be empty
      authenticationResponse.success.json[AuthResponse].token.value should not be empty

    }

  }

}
