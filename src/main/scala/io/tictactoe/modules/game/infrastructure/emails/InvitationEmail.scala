package io.tictactoe.modules.game.infrastructure.emails

import cats.effect.Sync
import io.tictactoe.modules.authentication.domain.services.LiveAuthEmail
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.logging.Logging
import cats.implicits._
import io.tictactoe.implicits._
import io.tictactoe.modules.game.infrastructure.emails.templates.{InvitationNotificationTemplateData, InvitationTemplateData}
import io.tictactoe.values.{Email, GameId, Link}
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.modules.users.model.DetailedUser
import io.tictactoe.utilities.emails.EmailSender

trait InvitationEmail[F[_]] {
  def sendInvitationEmail(recipient: Email, host: DetailedUser, gameId: GameId, token: ConfirmationToken): F[Unit]

  def sendInvitationNotification(host: DetailedUser, guest: DetailedUser, gameId: GameId): F[Unit]
}

object InvitationEmail {

  def live[F[_]: Configuration: Logging: Sync: EmailSender]: F[InvitationEmail[F]] = {
    for {
      configuration <- Configuration[F].access()
      logger <- Logging[F].create[LiveAuthEmail.type]
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
