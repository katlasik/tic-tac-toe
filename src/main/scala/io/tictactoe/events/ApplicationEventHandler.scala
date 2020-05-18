package io.tictactoe.events

import cats.effect.Sync
import io.tictactoe.utilities.events.EventHandler
import io.tictactoe.utilities.events.model.Event
import cats.implicits._
import io.tictactoe.implicits._
import io.tictactoe.modules.authentication.AuthenticationModule
import io.tictactoe.events.model.authentication.{PasswordChangedEvent, UserRegisteredEvent}
import io.tictactoe.events.model.game.GameInvitationAccepted
import io.tictactoe.modules.game.GameModule
import io.tictactoe.utilities.logging.{Logger, Logging}
import io.tictactoe.values.Unconfirmed

final class ApplicationEventHandler[F[_]: Sync](logger: Logger[F], authenticationModule: AuthenticationModule[F], gameModule: GameModule[F])
    extends EventHandler[F] {
  override def handle: PartialFunction[Event, F[Unit]] = {
    case UserRegisteredEvent(_, _, userId, username, email, Some(token), Unconfirmed) =>
      authenticationModule.registration.sendRegistrationConfirmationMail(email, username, userId, token)
    case e: UserRegisteredEvent => logger.info(show"User with id ${e.userId} already confirmed, not sending email.")
    case PasswordChangedEvent(_, _, _, username, email) =>
      authenticationModule.passwordChanger.sendPasswordChangedNotification(email, username)
    case GameInvitationAccepted(_, _, gameId, ownerId, guestId) =>
      gameModule.gameService.createGame(gameId, ownerId, guestId)

  }
}

object ApplicationEventHandler {
  def live[F[_]: Sync: Logging](authenticationModule: AuthenticationModule[F], gameModule: GameModule[F]): F[ApplicationEventHandler[F]] =
    for {
      logger <- Logging[F].create[ApplicationEventHandler[F]]
    } yield new ApplicationEventHandler[F](logger, authenticationModule, gameModule)
}
