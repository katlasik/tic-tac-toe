package io.tictactoe.utilities.emails.model

import cats.data.NonEmptyList
import io.tictactoe.utilities.emails.values.{EmailMessageText, EmailMessageTitle, MailId}
import io.tictactoe.values.Email

final case class MissingEmail(
    id: MailId,
    recipients: NonEmptyList[Email],
    sender: Email,
    text: EmailMessageText,
    title: EmailMessageTitle
)
