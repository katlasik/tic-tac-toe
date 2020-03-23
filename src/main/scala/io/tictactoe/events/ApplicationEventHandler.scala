package io.tictactoe.events

import io.tictactoe.authentication.events.UserRegisteredEvent
import io.tictactoe.authentication.services.AuthEmail
import io.tictactoe.events.bus.{Event, EventHandler}

class ApplicationEventHandler[F[_]: AuthEmail] extends EventHandler[F] {
  override def handle(event: Event): F[Unit] = event match {
    case UserRegisteredEvent(_, _, id, username, email, token) => AuthEmail[F].sendRegistrationConfirmation(email, username, id, token)
  }
}

object ApplicationEventHandler {

  def live[F[_]: AuthEmail]: ApplicationEventHandler[F] = new ApplicationEventHandler[F]

}
