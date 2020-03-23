package io.tictactoe.routes
import cats.effect.{ContextShift, Sync}
import io.tictactoe.authentication.services.{Authentication, PasswordChanger, Registration}
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._
import io.tictactoe.base.errors.ErrorMapper._
import cats.implicits._
import io.tictactoe.authentication.model.AuthResponse
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.authentication.values.AuthToken
import io.tictactoe.base.logging.Logging
import sttp.model.StatusCode

import scala.util.chaining._

object PublicRouter {

  def routes[F[_]: Sync: ContextShift: Registration: Authentication: AuthRepository: Logging: PasswordChanger]: HttpRoutes[F] = {

    val registerUser = Endpoints.registerUser.toRoutes(request => Registration[F].register(request).mapErrors)

    val login =
      Endpoints.login.toRoutes(
        request =>
          Authentication[F]
            .authenticate(request)
            .map(t => AuthToken(t.jwt.toEncodedString).pipe(t => (t, AuthResponse(t))))
            .mapErrors
      )

    val confirmRegistration = Endpoints.confirmRegistration.toRoutes{
      case (token, id) =>
        val result = for {
        redirectLocation <- Registration[F].confirm(token, id)
      } yield (StatusCode.SeeOther, redirectLocation)

      result.mapErrors
    }

    val resendConfirmationEmail = Endpoints.resendConfirmationEmail.toRoutes(Registration[F].resendEmail(_).mapErrors)

    val requestPasswordChange = Endpoints.requestPasswordChange.toRoutes(PasswordChanger[F].request(_).mapErrors)

    val changePassword = Endpoints.changePassword.toRoutes(PasswordChanger[F].changePassword(_).mapErrors)

    registerUser <+> login <+> confirmRegistration <+> resendConfirmationEmail <+> requestPasswordChange <+> changePassword
  }

}
