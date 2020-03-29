package io.tictactoe.emails.services

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import io.tictactoe.infrastructure.logging.Logging
import io.tictactoe.emails.model._
import io.tictactoe.infrastructure.configuration.Configuration
import io.tictactoe.infrastructure.emails.{EmailTransport, model}
import io.tictactoe.infrastructure.emails.model.EmailMessage
import io.tictactoe.infrastructure.emails.values.{EmailMessageText, EmailMessageTitle}
import io.tictactoe.values.Email

trait EmailSender[F[_]] {
  def send(email: EmailMessage): F[Unit]

  def sendForSingleRecipient(email: Email, emailMessageText: EmailMessageText, emailMessageTitle: EmailMessageTitle): F[Unit]

  def sendMissingEmails(): F[Unit]
}

object EmailSender {

  def apply[F[_]](implicit ev: EmailSender[F]): EmailSender[F] = ev

  def live[F[_]: Sync: Configuration: EmailRepository: Logging: EmailTransport]: F[EmailSender[F]] = {

    for {
      logger <- Logging[F].create[EmailSender[F]]
    } yield
      new EmailSender[F] {
        override def sendMissingEmails(): F[Unit] =
          for {
            _ <- logger.info("Checking for unsent emails.")
            emails <- EmailRepository[F].missingEmails()
            _ <- Sync[F].whenA(emails.nonEmpty)(logger.info(s"Sending missing registration emails for ${emails.size} users."))
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

        override def sendForSingleRecipient(email: Email, text: EmailMessageText, title: EmailMessageTitle): F[Unit] =
          for {
            configuration <- Configuration[F].access()
            _ <- send(
              model.EmailMessage(
                NonEmptyList.one(email),
                configuration.mail.noReplyAddress,
                text,
                title
              )
            )
          } yield ()
      }
  }

}
