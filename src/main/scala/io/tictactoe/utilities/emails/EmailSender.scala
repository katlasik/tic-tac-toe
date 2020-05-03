package io.tictactoe.utilities.emails

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.database.Database
import io.tictactoe.utilities.emails.model.EmailMessage
import io.tictactoe.utilities.emails.services.{LiveEmailRepository, LiveEmailTransport, LiveEmailSender}
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.templates.model.TemplateDataValues
import io.tictactoe.utilities.uuid.UUIDGenerator
import io.tictactoe.values.Email

trait EmailSender[F[_]] {
  def send(email: EmailMessage): F[Unit]

  def renderAndSend(email: Email, data: TemplateDataValues): F[Unit]

  def sendMissingEmails(): F[Unit]
}

object EmailSender {

  def apply[F[_]](implicit ev: EmailSender[F]): EmailSender[F] = ev

  def live[F[_]: Sync: Configuration: Logging: Database: UUIDGenerator]: F[EmailSender[F]] = {

    for {
      transport <- LiveEmailTransport.create
      emailRepository = LiveEmailRepository.postgresql
      sender <-  LiveEmailSender.create(emailRepository, transport)
    } yield sender
  }

}
