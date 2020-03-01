package io.tictactoe.authentication.events

import cats.Monad
import io.tictactoe.authentication.model.User
import io.tictactoe.base.uuid.UUIDGenerator
import io.tictactoe.calendar.Calendar
import io.tictactoe.events.bus.Event
import io.tictactoe.values.{Email, EventId, EventTimestamp, Username}
import cats.implicits._


final case class UserRegisteredEvent(override val eventId: EventId, override val eventTimestamp: EventTimestamp, username: Username, email: Email) extends Event

object UserRegisteredEvent {

  def create[F[_]: Monad: Calendar: UUIDGenerator](user: User): F[Event] = for {
    timestamp <- Calendar[F].now()
    id <- EventId.next[F]
  } yield UserRegisteredEvent(id, EventTimestamp(timestamp), user.username, user.email)

}
