package io.tictactoe.authentication.infrastructure.services


import io.tictactoe.values.{Email, Username}
import io.tictactoe.authentication.model.PasswordChangeRequest

trait PasswordChanger[F[_]] {
  def sendPasswordChangedNotification(email: Email, username: Username): F[Unit]

  def request(email: Email): F[Unit]

  def changePassword(request: PasswordChangeRequest): F[Unit]
}
