package io.tictactoe.base.errors

import cats.effect.Sync
import io.tictactoe.error.ErrorView
import cats.implicits._
import io.tictactoe.authentication.errors.{AccountNotConfirmed, ResourceNotFound, WrongCredentials}
import io.tictactoe.authorization.errors.AccessForbidden
import io.tictactoe.base.logging.Logging
import io.tictactoe.base.validation.ValidationError
import sttp.model.StatusCode
import tsec.mac.jca.MacVerificationError

object ErrorMapper {

  implicit class FExtensions[F[_]: Sync: Logging, A](f: F[A]) {
    def mapErrors: F[Either[(ErrorView, StatusCode), A]] =
      for {
        logger <- Logging[F].create[ErrorMapper.type]
        value <- f.attemptT.leftSemiflatMap {
          case e: MacVerificationError => Sync[F].pure((ErrorView.fromThrowable(e), StatusCode.Unauthorized))
          case WrongCredentials        => Sync[F].pure((ErrorView.fromThrowable(WrongCredentials), StatusCode.Unauthorized))
          case AccountNotConfirmed     => Sync[F].pure((ErrorView.fromThrowable(AccountNotConfirmed), StatusCode.Unauthorized))
          case e: ValidationError      => Sync[F].pure((ErrorView.fromThrowable(e), StatusCode.BadRequest))
          case AccessForbidden         => Sync[F].pure((ErrorView("Access to resource is forbidden!"), StatusCode.Forbidden))
          case ResourceNotFound        => Sync[F].pure((ErrorView("Can't find resource."), StatusCode.NotFound))
          case e =>
            logger.error("Uncaught error!", e) *> Sync[F].pure((ErrorView("Internal server error!"), StatusCode.InternalServerError))
        }.value
      } yield value

  }

}
