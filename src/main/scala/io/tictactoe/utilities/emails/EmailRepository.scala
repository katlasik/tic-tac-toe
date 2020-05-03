package io.tictactoe.utilities.emails

import io.tictactoe.utilities.emails.model.{EmailMessage, MissingEmail}
import io.tictactoe.utilities.emails.values.MailId

trait EmailRepository[F[_]] {
  def save(email: EmailMessage): F[MailId]

  def confirm(mailId: MailId): F[Unit]

  def missingEmails(): F[List[MissingEmail]]
}
