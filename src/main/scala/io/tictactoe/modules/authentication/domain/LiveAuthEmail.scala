package io.tictactoe.modules.authentication.domain

import cats.effect.Sync
import io.tictactoe.modules.authentication.api.AuthEmail
import io.tictactoe.modules.authentication.infrastructure.emails.{PasswordChangedMailTemplateData, PasswordResetMailTemplateData, RegistrationMailTemplateData}
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.emails.EmailSender
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, Link, UserId, Username}
import cats.implicits._
import io.tictactoe.implicits._

object LiveAuthEmail {

  def live[F[_]: Sync: Logging: Configuration: EmailSender]: F[AuthEmail[F]] = {

    for {
      configuration <- Configuration[F].access()
      logger <- Logging[F].create[LiveAuthEmail.type]
    } yield
      new AuthEmail[F] {

        def confirmationLink(token: ConfirmationToken, id: UserId): Link =
          Link(show"${configuration.app.publicUrl}/registration/confirmation?token=$token&id=$id")

        def passwordResetLink(token: ConfirmationToken, id: UserId): Link =
          Link(show"${configuration.app.publicUrl}/newpassword?token=$token&id=$id")

        override def sendRegistrationConfirmation(email: Email, username: Username, userId: UserId, token: ConfirmationToken): F[Unit] =
          for {
            _ <- logger.info(show"Sending registration confirmation email to $email.")
            _ <- EmailSender[F].renderAndSend(email, RegistrationMailTemplateData(username, confirmationLink(token, userId)))
          } yield ()

        override def sendPasswordChangeRequest(email: Email, username: Username, userId: UserId, token: ConfirmationToken): F[Unit] =
          for {
            _ <- logger.info(show"Sending password change request email to $email.")
            _ <- EmailSender[F].renderAndSend(email, PasswordResetMailTemplateData(username, passwordResetLink(token, userId)))
          } yield ()

        override def sendPasswordChangedNotification(email: Email, username: Username): F[Unit] =
          EmailSender[F]
            .renderAndSend(email, PasswordChangedMailTemplateData(username))
            .void
      }
  }
}
