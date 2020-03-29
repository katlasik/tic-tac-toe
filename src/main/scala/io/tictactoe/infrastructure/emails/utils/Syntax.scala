package io.tictactoe.infrastructure.emails.utils

import cats.effect.Sync
import io.tictactoe.infrastructure.emails.values.{EmailMessageText, EmailMessageTitle}
import io.tictactoe.values.Email
import javax.mail.Address
import javax.mail.internet.InternetAddress

object Syntax {

  implicit class EmailExtension(email: Email) {
    def toAddress[F[_]: Sync](): F[Address] = Sync[F].delay(new InternetAddress(email.value))
  }

  implicit class EmailMessageTextExtension(text: String) {
    def toEmailText: EmailMessageText = EmailMessageText(text)
  }

  implicit class EmailMessageTitleExtension(text: String) {
    def toEmailTitle: EmailMessageTitle = EmailMessageTitle(text)
  }

}
