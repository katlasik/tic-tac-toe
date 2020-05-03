package io.tictactoe.events

import cats.effect.Sync
import io.tictactoe.utilities.events.EventHandler
import io.tictactoe.utilities.events.model.Event
import cats.implicits._
import io.tictactoe.authentication.AuthenticationModule
import io.tictactoe.events.model.authentication.{PasswordChangedEvent, UserRegisteredEvent}
import io.tictactoe.utilities.logging.{Logger, Logging}
import io.tictactoe.values.Unconfirmed

final class ApplicationEventHandler[F[_]: Sync](logger: Logger[F], authenticationModule: AuthenticationModule[F]) extends EventHandler[F] {
  override def handle(event: Event): F[Unit] = event match {
    case UserRegisteredEvent(_, _, userId, username, email, Some(token), Unconfirmed) =>
      authenticationModule.registration.sendRegistrationConfirmationMail(email, username, userId, token)
    case e: UserRegisteredEvent => logger.info(show"User with id ${e.userId} already confirmed, not sending email.")
    case PasswordChangedEvent(_, _, _, username, email) =>
      authenticationModule.passwordChanger.sendPasswordChangedNotification(email, username)
  }
}

object ApplicationEventHandler {
  def live[F[_]: Sync: Logging](authenticationModule: AuthenticationModule[F]): F[ApplicationEventHandler[F]] =
    for {
      logger <- Logging[F].create[ApplicationEventHandler[F]]
    } yield new ApplicationEventHandler[F](logger, authenticationModule)
}
