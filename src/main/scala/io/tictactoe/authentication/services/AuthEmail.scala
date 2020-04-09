package io.tictactoe.authentication.services

import cats.effect.Sync
import io.tictactoe.infrastructure.logging.Logging
import io.tictactoe.values.{Email, Link, UserId, Username}
import cats.implicits._
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.authentication.emails.{PasswordChangedMailTemplateData, PasswordResetMailTemplateData, RegistrationMailTemplateData}
import io.tictactoe.infrastructure.templates.TemplateRenderer
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.emails.services.EmailSender
import io.tictactoe.infrastructure.configuration.Configuration

trait AuthEmail[F[_]] {
  def sendRegistrationConfirmation(email: Email, username: Username, id: UserId, token: ConfirmationToken): F[Unit]

  def sendPasswordChangeRequest(email: Email, username: Username, id: UserId, token: ConfirmationToken): F[Unit]

  def sendPasswordChangedNotification(email: Email, username: Username): F[Unit]
}

object AuthEmail {

  def apply[F[_]](implicit ev: AuthEmail[F]): AuthEmail[F] = ev

  def live[F[_]: Sync: Logging: EmailSender: Configuration: TemplateRenderer: AuthRepository]: F[AuthEmail[F]] = {

    for {
      configuration <- Configuration[F].access()
      logger <- Logging[F].create[AuthEmail.type]
    } yield
      new AuthEmail[F] {

        def confirmationLink(token: ConfirmationToken, id: UserId): Link =
          Link(show"${configuration.app.publicUrl}/registration?token=$token&id=$id")

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
