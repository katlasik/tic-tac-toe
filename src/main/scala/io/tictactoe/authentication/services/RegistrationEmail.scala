package io.tictactoe.authentication.services

import cats.data.NonEmptyList
import cats.effect.Sync
import io.tictactoe.base.logging.Logging
import io.tictactoe.values.{Email, UserId, Username}
import cats.implicits._
import io.tictactoe.authentication.model.ConfirmationToken
import io.tictactoe.authentication.templates.RegistrationMailTemplateData
import io.tictactoe.base.templates.TemplateRenderer
import io.tictactoe.base.templates.model.RenderedTemplate
import io.tictactoe.configuration.Configuration
import io.tictactoe.emails.EmailMessage
import io.tictactoe.emails.services.EmailSender
import io.tictactoe.emails.services.values.{EmailMessageText, EmailMessageTitle}

trait RegistrationEmail[F[_]] {
  def send(email: Email, username: Username, id: UserId, token: ConfirmationToken): F[Unit]
}

object RegistrationEmail {

  def apply[F[_]](implicit ev: RegistrationEmail[F]): RegistrationEmail[F] = ev

  def live[F[_]: Sync: Logging: EmailSender: Configuration: TemplateRenderer]: F[RegistrationEmail[F]] = {

    for {
      configuration <- Configuration[F].access()
      logger <- Logging[F].create[RegistrationEmail.type]
    } yield
      new RegistrationEmail[F] {

        def confirmationLink(token: ConfirmationToken, id: UserId): String =
          show"${configuration.app.publicUrl}/registration?token=$token&id=$id"

        override def send(email: Email, username: Username, id: UserId, token: ConfirmationToken): F[Unit] =
          for {
            _ <- logger.info(show"Sending registration confirmation email to $email.")
            RenderedTemplate(title, text) <- TemplateRenderer[F].renderTemplateAndTitle(
              RegistrationMailTemplateData(username.value, confirmationLink(token, id))
            )
            _ <- EmailSender[F].send(
              EmailMessage(
                NonEmptyList.one(email),
                configuration.mail.noReplyAddress,
                EmailMessageText(text),
                EmailMessageTitle(title)
              )
            )
          } yield ()
      }
  }
}
