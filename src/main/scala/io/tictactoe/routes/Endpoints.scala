package io.tictactoe.routes

import io.tictactoe.authentication.model.{AuthResponse, Credentials, PasswordChangeRequest,RegistrationRequest, RegistrationResult}
import io.tictactoe.error.ErrorView
import sttp.tapir.json.circe._
import sttp.tapir._
import io.circe.generic.auto._
import io.tictactoe.authentication.values.AuthToken
import io.tictactoe.base.model.RedirectLocation
import io.tictactoe.base.tokens.values.ConfirmationToken
import io.tictactoe.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.values.{Email, UserId}
import sttp.model.StatusCode

object Endpoints {

  val registerUser: Endpoint[RegistrationRequest, (ErrorView, StatusCode), RegistrationResult, Nothing] = endpoint
    .description("Endpoint for registering new users.")
    .post
    .in("registration")
    .in(jsonBody[RegistrationRequest])
    .errorOut(jsonBody[ErrorView].description("Error message."))
    .errorOut(statusCode)
    .out(jsonBody[RegistrationResult])

  val confirmRegistration: Endpoint[(ConfirmationToken, UserId), (ErrorView, StatusCode), (StatusCode, RedirectLocation), Nothing] =
    endpoint
      .description("Endpoint for confirming registration.")
      .get
      .in("registration")
      .in(query[ConfirmationToken]("token"))
      .description("Random token.")
      .in(query[UserId]("id"))
      .description("Id of user confirming registration.")
      .out(statusCode)
      .out(header[RedirectLocation]("Location"))
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)

  val resendConfirmationEmail: Endpoint[Email, (ErrorView, StatusCode), Unit, Nothing] = endpoint
    .description("Endpoint for requesting resending of registration email.")
    .put
    .in("registration")
    .in(query[Email]("email"))
    .description("Email of user, which requests resending of confirmation email.")
    .errorOut(jsonBody[ErrorView].description("Error message."))
    .errorOut(statusCode)

  val requestPasswordChange: Endpoint[Email, (ErrorView, StatusCode), Unit, Nothing] = endpoint
    .description("Endpoint for requesting sending of mail for token.")
    .post
    .in("password")
    .in(query[Email]("email"))
    .description("Email of user, which requests resending of confirmation email.")
    .errorOut(jsonBody[ErrorView].description("Error message."))
    .errorOut(statusCode)

  val changePassword: Endpoint[PasswordChangeRequest, (ErrorView, StatusCode), Unit, Nothing] = endpoint
    .description("Endpoint for requesting changing of password.")
    .post
    .in("password" / "change")
    .in(jsonBody[PasswordChangeRequest])
    .errorOut(jsonBody[ErrorView].description("Error message."))
    .errorOut(statusCode)

  val login: Endpoint[Credentials, (ErrorView, StatusCode), (AuthToken, AuthResponse), Nothing] = endpoint
    .description("Endpoint for login in.")
    .post
    .in("login")
    .in(jsonBody[Credentials].description("User's credentials."))
    .errorOut(jsonBody[ErrorView].description("Error message."))
    .errorOut(statusCode)
    .out(header[AuthToken]("Set-Auth-Token").description("Authentication token."))
    .out(jsonBody[AuthResponse].description("Authentication token."))

  val getUsers: Endpoint[String, (ErrorView, StatusCode), List[SimpleUser], Nothing] = endpoint
    .description("Endpoint for getting users' details.")
    .get
    .in("users")
    .in(auth.bearer)
    .out(jsonBody[List[SimpleUser]].description("List of users."))
    .errorOut(jsonBody[ErrorView].description("Error message."))
    .errorOut(statusCode)

  val getUser: Endpoint[(UserId, String), (ErrorView, StatusCode), DetailedUser, Nothing] = endpoint
    .description("Endpoint for getting user's details.")
    .get
    .in("users" / path[UserId].name("id").description("The id of the user."))
    .in(auth.bearer)
    .out(jsonBody[DetailedUser].description("Details of single user."))
    .errorOut(jsonBody[ErrorView].description("Error message."))
    .errorOut(statusCode)

  val all: List[Endpoint[_, _, _, _]] =
    List(registerUser, confirmRegistration, resendConfirmationEmail, requestPasswordChange, login, getUsers, getUser, changePassword)

}
