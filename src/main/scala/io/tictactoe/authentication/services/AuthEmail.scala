package io.tictactoe.authentication.services

import cats.effect.Sync
import io.tictactoe.infrastructure.logging.Logging
import io.tictactoe.values.{Email, UserId, Username}
import cats.implicits._
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.authentication.templates.{PasswordChangedMailTemplateData, PasswordResetMailTemplateData, RegistrationMailTemplateData}
import io.tictactoe.infrastructure.templates.TemplateRenderer
import io.tictactoe.infrastructure.templates.model.{RenderedTemplate, TemplateData}
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.emails.services.EmailSender
import io.tictactoe.infrastructure.configuration.Configuration
import io.tictactoe.infrastructure.emails.utils.Syntax._

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

        def confirmationLink(token: ConfirmationToken, id: UserId): String =
          show"${configuration.app.publicUrl}/registration?token=$token&id=$id"

        def passwordResetLink(token: ConfirmationToken, id: UserId): String =
          show"${configuration.app.publicUrl}/newpassword?token=$token&id=$id"

        def sendMail(data: TemplateData, email: Email): F[Unit] =
          for {
            RenderedTemplate(title, text) <- TemplateRenderer[F].renderTemplateAndTitle(data)
            _ <- EmailSender[F].sendForSingleRecipient(email, text.toEmailText, title.toEmailTitle)
          } yield ()

        override def sendRegistrationConfirmation(email: Email, username: Username, userId: UserId, token: ConfirmationToken): F[Unit] =
          for {
            _ <- logger.info(show"Sending registration confirmation email to $email.")
            _ <- sendMail(
              RegistrationMailTemplateData(username.value, confirmationLink(token, userId)),
              email
            )
          } yield ()

        override def sendPasswordChangeRequest(email: Email, username: Username, userId: UserId, token: ConfirmationToken): F[Unit] =
          for {
            _ <- logger.info(show"Sending password change request email to $email.")
            _ <- sendMail(
              PasswordResetMailTemplateData(username.value, passwordResetLink(token, userId)),
              email
            )
          } yield ()

        override def sendPasswordChangedNotification(email: Email, username: Username): F[Unit] =
          sendMail(
            PasswordChangedMailTemplateData(username.value),
            email
          ).void
      }
  }
}
