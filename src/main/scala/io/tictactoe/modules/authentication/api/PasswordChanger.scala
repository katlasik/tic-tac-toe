package io.tictactoe.modules.authentication.api

import io.tictactoe.modules.authentication.model.PasswordChangeRequest
import io.tictactoe.values.{Email, Username}

trait PasswordChanger[F[_]] {
  def sendPasswordChangedNotification(email: Email, username: Username): F[Unit]

  def request(email: Email): F[Unit]

  def changePassword(request: PasswordChangeRequest): F[Unit]
}
