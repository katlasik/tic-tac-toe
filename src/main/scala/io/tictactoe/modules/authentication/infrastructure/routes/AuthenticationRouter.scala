package io.tictactoe.modules.authentication.infrastructure.routes

import cats.data.NonEmptyList
import cats.effect.{ContextShift, Sync}
import io.tictactoe.modules.authentication.model.{AuthResponse, Credentials, PasswordChangeRequest, RawRegistrationRequest, RegistrationResult}
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.values.{AuthToken, Email, UserId}
import sttp.tapir.server.http4s._
import cats.implicits._
import io.tictactoe.errors.ErrorView
import io.tictactoe.implicits._
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import sttp.tapir.json.circe.jsonBody
import sttp.tapir._
import io.circe.generic.auto._
import io.tictactoe.modules.authentication.api.{Authenticator, PasswordChanger, Registration}
import io.tictactoe.utilities.routes.Router
import sttp.tapir.server.ServerEndpoint

import scala.util.chaining._

class AuthenticationRouter[F[_]: Sync: ContextShift: Http4sServerOptions: Logging](
    registration: Registration[F],
    authentication: Authenticator[F],
    passwordChanger: PasswordChanger[F]
) extends Router[F] {

  override def serverEndpoints: NonEmptyList[ServerEndpoint[_, _, _, Nothing, F]] = {

    val registerUser = endpoint
      .description("Endpoint for registering new users.")
      .post
      .in("registration")
      .in(jsonBody[RawRegistrationRequest])
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .out(jsonBody[RegistrationResult])
      .serverLogic(request => registration.register(request).mapErrors)

    val confirmRegistration =
      endpoint
        .description("Endpoint for confirming registration.")
        .post
        .in("registration" / "confirmation")
        .in(query[ConfirmationToken]("token"))
        .description("Random token.")
        .in(query[UserId]("id").description("The id of user confirming registration."))
        .errorOut(jsonBody[ErrorView].description("Error message."))
        .errorOut(statusCode)
        .serverLogic {
          case (token, id) =>
            val result = for {
              _ <- registration.confirm(token, id)
            } yield ()

            result.mapErrors
        }

    val resendConfirmationEmail = endpoint
      .description("Endpoint for requesting resending of registration email.")
      .put
      .in("registration")
      .in(query[Email]("email").description("Email of user, which requests resending of confirmation email."))
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .serverLogic(registration.resendEmail(_).mapErrors)

    val requestPasswordChange = endpoint
      .description("Endpoint for requesting sending of mail for token.")
      .put
      .in("password")
      .in(query[Email]("email").description("Email of user, which requests changing of password."))
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .serverLogic(passwordChanger.request(_).mapErrors)

    val changePassword = endpoint
      .description("Endpoint for password's changing.")
      .post
      .in("password" / "change")
      .in(jsonBody[PasswordChangeRequest])
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .serverLogic(passwordChanger.changePassword(_).mapErrors)

    val login = endpoint
      .description("Endpoint for login in.")
      .post
      .in("login")
      .in(jsonBody[Credentials].description("User's credentials."))
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .out(header[AuthToken]("Set-Auth-Token").description("Authentication token."))
      .out(jsonBody[AuthResponse].description("Authentication token."))
      .serverLogic(
        request =>
          authentication
            .authenticate(request)
            .map(t => AuthToken(t.jwt.toEncodedString).pipe(t => (t, AuthResponse(t))))
            .mapErrors
      )

    NonEmptyList.of(registerUser, login, confirmRegistration, resendConfirmationEmail, requestPasswordChange, changePassword)
  }

}
