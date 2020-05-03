package io.tictactoe.utilities.emails

import io.tictactoe.utilities.emails.model.EmailMessage

trait EmailTransport[F[_]] {
  def send(email: EmailMessage): F[Unit]
}
