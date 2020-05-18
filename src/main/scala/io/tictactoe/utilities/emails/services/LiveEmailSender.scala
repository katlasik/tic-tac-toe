package io.tictactoe.utilities.emails.services

import cats.data.NonEmptyList
import cats.effect.Sync
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.emails.model.{EmailMessage, MissingEmail}
import io.tictactoe.utilities.emails.{EmailRepository, EmailSender, EmailTransport, model}
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.templates.TemplateRenderer
import io.tictactoe.utilities.templates.model.{RenderedTemplate, TemplateDataValues}
import io.tictactoe.values.Email
import cats.implicits._
import io.tictactoe.implicits._

object LiveEmailSender {

  def create[F[_]: Logging: Configuration: Sync](
      emailRepository: EmailRepository[F],
      emailTransport: EmailTransport[F]
  ): F[EmailSender[F]] =
    for {
      logger <- Logging[F].create[EmailSender[F]]
      configuration <- Configuration[F].access()
      templateRenderer <- TemplateRenderer.live[F]
    } yield
      new EmailSender[F] {
        override def sendMissingEmails(): F[Unit] =
          for {
            _ <- logger.info("Running check for unsent emails.")
            emails <- emailRepository.missingEmails()
            _ <- logger.info(s"Sending missing registration emails for ${emails.size} users.").whenA(emails.nonEmpty)
            _ <- emails.traverse {
              case MissingEmail(id, recipients, sender, text, title) =>
                for {
                  _ <- emailTransport.send(model.EmailMessage(recipients, sender, text, title))
                  _ <- emailRepository.confirm(id)
                } yield ()
            }
          } yield ()

        override def send(message: EmailMessage): F[Unit] =
          for {
            id <- emailRepository.save(message)
            _ <- emailTransport.send(message)
            _ <- emailRepository.confirm(id)
          } yield ()

        override def renderAndSend(email: Email, data: TemplateDataValues): F[Unit] =
          for {
            RenderedTemplate(title, text) <- templateRenderer.renderTemplateAndTitle(data)
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
