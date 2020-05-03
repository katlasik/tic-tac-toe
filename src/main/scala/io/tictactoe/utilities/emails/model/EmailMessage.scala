package io.tictactoe.utilities.emails.model

import cats.data.NonEmptyList
import io.tictactoe.utilities.emails.values.{EmailMessageText, EmailMessageTitle}
import io.tictactoe.values.Email

final case class EmailMessage(recipients: NonEmptyList[Email], sender: Email, text: EmailMessageText, title: EmailMessageTitle)
