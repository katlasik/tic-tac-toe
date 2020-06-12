package io.tictactoe.modules.authentication.api

import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, UserId, Username}

trait AuthEmail[F[_]] {
  def sendRegistrationConfirmation(email: Email, username: Username, id: UserId, token: ConfirmationToken): F[Unit]

  def sendPasswordChangeRequest(email: Email, username: Username, id: UserId, token: ConfirmationToken): F[Unit]

  def sendPasswordChangedNotification(email: Email, username: Username): F[Unit]
}
