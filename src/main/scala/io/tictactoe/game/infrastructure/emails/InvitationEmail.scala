package io.tictactoe.game.infrastructure.emails

import cats.effect.Sync
import io.tictactoe.authentication.services.AuthEmail
import io.tictactoe.infrastructure.configuration.Configuration
import io.tictactoe.infrastructure.logging.Logging
import cats.implicits._
import io.tictactoe.emails.services.EmailSender
import io.tictactoe.game.infrastructure.emails.templates.{InvitationNotificationTemplateData, InvitationTemplateData}
import io.tictactoe.game.values.GameId
import io.tictactoe.infrastructure.templates.TemplateRenderer
import io.tictactoe.values.{Email, Link}
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.users.model.DetailedUser

trait InvitationEmail[F[_]] {
  def sendInvitationEmail(recipient: Email, host: DetailedUser, gameId: GameId, token: ConfirmationToken): F[Unit]

  def sendInvitationNotification(host: DetailedUser, guest: DetailedUser, gameId: GameId): F[Unit]
}

object InvitationEmail {

  def apply[F[_]](implicit ev: InvitationEmail[F]): InvitationEmail[F] = ev

  def live[F[_]: Configuration: Logging: TemplateRenderer: EmailSender: Sync]: F[InvitationEmail[F]] = {
    for {
      configuration <- Configuration[F].access()
      logger <- Logging[F].create[AuthEmail.type]
    } yield {

      new InvitationEmail[F] {

        override def sendInvitationEmail(recipient: Email, host: DetailedUser, gameId: GameId, token: ConfirmationToken): F[Unit] =
          for {
            _ <- logger.info(show"Sending invitation mail to $recipient from user with id ${host.id}.")
            _ <- EmailSender[F].renderAndSend(
              recipient,
              InvitationTemplateData(
                host.username,
                Link(show"${configuration.app.publicUrl}/games/invitation?token=$token&id=$gameId")
              )
            )
          } yield ()

        override def sendInvitationNotification(host: DetailedUser, guest: DetailedUser, gameId: GameId): F[Unit] =
          for {
            _ <- logger.info(show"Sending invitation notification mail to user with id ${guest.id} from user with id ${host.id}.")
            _ <- EmailSender[F].renderAndSend(
              guest.email,
              InvitationNotificationTemplateData(
                host.username,
                guest.username,
                Link(show"${configuration.app.publicUrl}/games/invitation?&id=$gameId")
              )
            )
          } yield ()
      }
    }
  }

}
