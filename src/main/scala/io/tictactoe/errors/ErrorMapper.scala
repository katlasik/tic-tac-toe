package io.tictactoe

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.errors.{ErrorView, ResourceNotFound}
import io.tictactoe.modules.authentication.errors.{AccountNotConfirmed, WrongCredentials}
import io.tictactoe.modules.game.errors.{InviteSelfError, UserAlreadyExistsError}
import io.tictactoe.utilities.authorization.errors.AccessForbidden
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.validation.ValidationError
import sttp.model.StatusCode
import tsec.mac.jca.MacVerificationError

trait ErrorMapper {
  implicit class FExtensions[F[_]: Sync: Logging, A](f: F[A]) {
    def mapErrors: F[Either[(ErrorView, StatusCode), A]] =
      for {
        logger <- Logging[F].create[ErrorMapper]
        value <- f.attemptT.leftSemiflatMap {
          case e: MacVerificationError   => (ErrorView.fromThrowable(e), StatusCode.Unauthorized).pure[F]
          case WrongCredentials          => (ErrorView.fromThrowable(WrongCredentials), StatusCode.Unauthorized).pure[F]
          case AccountNotConfirmed       => (ErrorView.fromThrowable(AccountNotConfirmed), StatusCode.Unauthorized).pure[F]
          case e: ValidationError        => (ErrorView.fromThrowable(e), StatusCode.BadRequest).pure[F]
          case AccessForbidden           => (ErrorView("Access to resource is forbidden!"), StatusCode.Forbidden).pure[F]
          case ResourceNotFound          => (ErrorView("Can't find resource."), StatusCode.NotFound).pure[F]
          case e: UserAlreadyExistsError => (ErrorView.fromThrowable(e), StatusCode.BadRequest).pure[F]
          case InviteSelfError           => (ErrorView.fromThrowable(InviteSelfError), StatusCode.BadRequest).pure[F]
          case e =>
            logger.error("Uncaught error!", e) *> (ErrorView("Internal server error!"), StatusCode.InternalServerError).pure[F]
        }.value
      } yield value
  }
}
