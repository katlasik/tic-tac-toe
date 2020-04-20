package io.tictactoe.events

import cats.effect.Sync
import io.tictactoe.authentication.events.{PasswordChangedEvent, UserRegisteredEvent}
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.authentication.services.AuthEmail
import io.tictactoe.error.IllegalApplicationState
import io.tictactoe.infrastructure.events.EventHandler
import io.tictactoe.infrastructure.events.model.Event
import io.tictactoe.infrastructure.syntax._
import cats.implicits._
import io.tictactoe.infrastructure.logging.{Logger, Logging}
import io.tictactoe.values.Unconfirmed

final class ApplicationEventHandler[F[_]: AuthEmail: AuthRepository: Sync](logger: Logger[F]) extends EventHandler[F] {
  override def handle(event: Event): F[Unit] = event match {
    case UserRegisteredEvent(_, _, userId, username, email, Some(token), Unconfirmed) =>
      AuthEmail[F].sendRegistrationConfirmation(email, username, userId, token)
    case e: UserRegisteredEvent => logger.info(show"User with id ${e.userId} already confirmed, not sending email.")
    case PasswordChangedEvent(_, _, userId) =>
      for {
        user <- AuthRepository[F].getById(userId).throwIfEmpty(IllegalApplicationState(show"Can't find user with id = $userId."))
        _ <- AuthEmail[F].sendPasswordChangedNotification(user.email, user.username)
      } yield ()
  }
}

object ApplicationEventHandler {
  def live[F[_]: AuthEmail: AuthRepository: Sync: Logging]: F[ApplicationEventHandler[F]] =
    for {
      logger <- Logging[F].create[ApplicationEventHandler[F]]
    } yield new ApplicationEventHandler[F](logger)
}
