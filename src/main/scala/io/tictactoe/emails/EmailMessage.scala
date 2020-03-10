package io.tictactoe.emails

import cats.data.NonEmptyList
import io.tictactoe.emails.services.values.{EmailMessageText, EmailMessageTitle}
import io.tictactoe.values.Email

final case class EmailMessage(to: NonEmptyList[Email], from: Email, text: EmailMessageText, title: EmailMessageTitle)
