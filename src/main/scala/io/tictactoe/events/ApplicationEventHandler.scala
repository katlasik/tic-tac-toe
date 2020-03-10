package io.tictactoe.events

import io.tictactoe.authentication.events.UserRegisteredEvent
import io.tictactoe.authentication.services.RegistrationEmail
import io.tictactoe.events.bus.{Event, EventHandler}

class ApplicationEventHandler[F[_]: RegistrationEmail] extends EventHandler[F] {
  override def handle(event: Event): F[Unit] = event match {
    case UserRegisteredEvent(_, _, id, username, email, token) => RegistrationEmail[F].send(email, username, id, token)
  }
}

object ApplicationEventHandler {

  def live[F[_]: RegistrationEmail]: ApplicationEventHandler[F] = new ApplicationEventHandler[F]

}
