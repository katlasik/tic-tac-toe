package io.tictactoe.events

import cats.effect.Sync
import io.tictactoe.authentication.events.{PasswordChangedEvent, UserRegisteredEvent}
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.authentication.services.AuthEmail
import io.tictactoe.error.IllegalApplicationState
import io.tictactoe.infrastructure.events.EventHandler
import io.tictactoe.infrastructure.events.model.Event
import io.tictactoe.infrastructure.utils.Syntax._
import cats.implicits._

class ApplicationEventHandler[F[_]: AuthEmail: AuthRepository: Sync] extends EventHandler[F] {
  override def handle(event: Event): F[Unit] = event match {
    case UserRegisteredEvent(_, _, userId, username, email, token) =>
      AuthEmail[F].sendRegistrationConfirmation(email, username, userId, token)
    case PasswordChangedEvent(_, _, userId) =>
      for {
        user <- AuthRepository[F].getById(userId).throwIfEmpty(IllegalApplicationState(show"Can't find user with id = $userId."))
        _ <- AuthEmail[F].sendPasswordChangedNotification(user.email, user.username)
      } yield ()
  }
}

object ApplicationEventHandler {
  def live[F[_]: AuthEmail: AuthRepository: Sync]: ApplicationEventHandler[F] = new ApplicationEventHandler[F]
}
