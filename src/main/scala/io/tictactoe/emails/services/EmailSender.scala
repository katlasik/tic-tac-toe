package io.tictactoe.emails.services

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import io.tictactoe.infrastructure.logging.Logging
import io.tictactoe.emails.model._
import io.tictactoe.infrastructure.configuration.Configuration
import io.tictactoe.infrastructure.emails.{EmailTransport, model}
import io.tictactoe.infrastructure.emails.model.EmailMessage
import io.tictactoe.infrastructure.templates.TemplateRenderer
import io.tictactoe.infrastructure.templates.model.{RenderedTemplate, TemplateDataValues}
import io.tictactoe.values.Email
import io.tictactoe.infrastructure.emails.syntax._

trait EmailSender[F[_]] {
  def send(email: EmailMessage): F[Unit]

  def renderAndSend(email: Email, data: TemplateDataValues): F[Unit]

  def sendMissingEmails(): F[Unit]
}

object EmailSender {

  def apply[F[_]](implicit ev: EmailSender[F]): EmailSender[F] = ev

  def live[F[_]: Sync: Configuration: EmailRepository: Logging: EmailTransport: TemplateRenderer]: F[EmailSender[F]] = {

    for {
      logger <- Logging[F].create[EmailSender[F]]
      configuration <- Configuration[F].access()
    } yield
      new EmailSender[F] {
        override def sendMissingEmails(): F[Unit] =
          for {
            _ <- logger.info("Running check for unsent emails.")
            emails <- EmailRepository[F].missingEmails()
            _ <- logger.info(s"Sending missing registration emails for ${emails.size} users.").whenA(emails.nonEmpty)
            _ <- emails.traverse {
              case MissingEmail(id, recipients, sender, text, title) =>
                for {
                  _ <- EmailTransport[F].send(model.EmailMessage(recipients, sender, text, title))
                  _ <- EmailRepository[F].confirm(id)
                } yield ()
            }
          } yield ()

        override def send(message: EmailMessage): F[Unit] =
          for {
            id <- EmailRepository[F].save(message)
            _ <- EmailTransport[F].send(message)
            _ <- EmailRepository[F].confirm(id)
          } yield ()

        override def renderAndSend(email: Email, data: TemplateDataValues): F[Unit] =
          for {
            RenderedTemplate(title, text) <- TemplateRenderer[F].renderTemplateAndTitle(data)
            _ <- send(
              EmailMessage(
                NonEmptyList.one(email),
                configuration.mail.noReplyAddress,
                text.toEmailText,
                title.toEmailTitle
              )
            )
          } yield ()
      }
  }

}
