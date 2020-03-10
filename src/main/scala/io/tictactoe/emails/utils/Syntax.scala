package io.tictactoe.emails.utils

import cats.effect.Sync
import io.tictactoe.values.Email
import javax.mail.Address
import javax.mail.internet.InternetAddress

object Syntax {

  implicit class EmailExtension(email: Email) {
    def toAddress[F[_]: Sync](): F[Address] = Sync[F].delay(new InternetAddress(email.value))
  }

}
