package io.tictactoe.modules.authentication.infrastructure.services


import io.tictactoe.values.{Email, Username}
import io.tictactoe.modules.authentication.model.PasswordChangeRequest

trait PasswordChanger[F[_]] {
  def sendPasswordChangedNotification(email: Email, username: Username): F[Unit]

  def request(email: Email): F[Unit]

  def changePassword(request: PasswordChangeRequest): F[Unit]
}
