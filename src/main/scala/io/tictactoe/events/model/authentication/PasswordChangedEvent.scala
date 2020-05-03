package io.tictactoe.events.model.authentication

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.authentication.model.User
import io.tictactoe.utilities.calendar.Calendar
import io.tictactoe.utilities.events.model.Event
import io.tictactoe.utilities.uuid.UUIDGenerator
import io.tictactoe.values._

final case class PasswordChangedEvent(
    override val eventId: EventId,
    override val eventTimestamp: EventTimestamp,
    userId: UserId,
    username: Username,
    email: Email
) extends Event

object PasswordChangedEvent {

  def create[F[_]: Sync: Calendar: UUIDGenerator](user: User): F[Event] =
    for {
      timestamp <- Calendar[F].now()
      id <- EventId.next[F]
    } yield PasswordChangedEvent(id, EventTimestamp(timestamp), user.id, user.username, user.email)

}
