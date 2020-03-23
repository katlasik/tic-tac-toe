package io.tictactoe.emails.model

import cats.data.NonEmptyList
import io.tictactoe.emails.values.{EmailMessageText, EmailMessageTitle}
import io.tictactoe.values.Email

final case class EmailMessage(recipients: NonEmptyList[Email], sender: Email, text: EmailMessageText, title: EmailMessageTitle)

