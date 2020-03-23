package io.tictactoe.authentication.services

import cats.effect.Sync
import io.tictactoe.base.logging.Logging
import io.tictactoe.values.{Email, UserId, Username}
import cats.implicits._
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.authentication.templates.{PasswordResetMailTemplateData, RegistrationMailTemplateData}
import io.tictactoe.base.templates.TemplateRenderer
import io.tictactoe.base.templates.model.{RenderedTemplate, TemplateData}
import io.tictactoe.base.tokens.values.ConfirmationToken
import io.tictactoe.base.uuid.UUIDGenerator
import io.tictactoe.configuration.Configuration
import io.tictactoe.emails.services.EmailSender
import io.tictactoe.emails.values.MailId
import io.tictactoe.emails.values.EmailMessageText._
import io.tictactoe.emails.values.EmailMessageTitle._

trait AuthEmail[F[_]] {
  def sendRegistrationConfirmation(email: Email, username: Username, id: UserId, token: ConfirmationToken): F[Unit]

  def sendPasswordChangeRequest(email: Email, username: Username, id: UserId, token: ConfirmationToken): F[Unit]
}

object AuthEmail {

  def apply[F[_]](implicit ev: AuthEmail[F]): AuthEmail[F] = ev

  def live[F[_]: UUIDGenerator: Sync: Logging: EmailSender: Configuration: TemplateRenderer: AuthRepository]: F[AuthEmail[F]] = {

    for {
      configuration <- Configuration[F].access()
      logger <- Logging[F].create[AuthEmail.type]
    } yield
      new AuthEmail[F] {

        def confirmationLink(token: ConfirmationToken, id: UserId): String =
          show"${configuration.app.publicUrl}/registration?token=$token&id=$id"

        def passwordResetLink(token: ConfirmationToken, id: UserId): String =
          show"${configuration.app.publicUrl}/newpassword?token=$token&id=$id"

        def sendMail(data: TemplateData, email: Email): F[MailId] =
          for {
            RenderedTemplate(title, text) <- TemplateRenderer[F].renderTemplateAndTitle(data)
            mailId <- MailId.next
            _ <- EmailSender[F].sendForSingleRecipient(email, text.toEmailText, title.toEmailTitle)
          } yield mailId


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

      }
  }
}
