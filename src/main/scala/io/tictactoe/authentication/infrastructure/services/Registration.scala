package io.tictactoe.authentication.infrastructure.services

import io.tictactoe.authentication.model.{RawRegistrationRequest, RegistrationResult}
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, UserId, Username}

trait Registration[F[_]] {
  def sendRegistrationConfirmationMail(email: Email, username: Username, userId: UserId, token: ConfirmationToken): F[Unit]

  def register(request: RawRegistrationRequest): F[RegistrationResult]

  def confirm(token: ConfirmationToken, id: UserId): F[Unit]

  def resendEmail(email: Email): F[Unit]
}
