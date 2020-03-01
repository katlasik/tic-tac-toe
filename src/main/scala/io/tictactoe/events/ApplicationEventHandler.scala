package io.tictactoe.events

import io.tictactoe.authentication.events.UserRegisteredEvent
import io.tictactoe.authentication.services.RegistrationEmail
import io.tictactoe.events.bus.{Event, EventHandler}

class ApplicationEventHandler[F[_]: RegistrationEmail] extends EventHandler[F]{
  override def handle(event: Event): F[Unit] = event match {
    case UserRegisteredEvent(_, _, username, email) => RegistrationEmail[F].send(email, username)
  }
}

object ApplicationEventHandler {

  def live[F[_]: RegistrationEmail]: ApplicationEventHandler[F] = new ApplicationEventHandler[F]

}
