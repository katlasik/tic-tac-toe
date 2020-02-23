package io.tictactoe.routes

import io.tictactoe.authentication.model.{AuthResponse, AuthToken, Credentials, RegistrationRequest, RegistrationResult}
import io.tictactoe.error.ErrorView
import sttp.tapir.json.circe._
import sttp.tapir._
import io.circe.generic.auto._
import io.tictactoe.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.values.UserId
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

  val all: List[Endpoint[_, _, _, _]] = List(registerUser, login, getUsers, getUser)

}
