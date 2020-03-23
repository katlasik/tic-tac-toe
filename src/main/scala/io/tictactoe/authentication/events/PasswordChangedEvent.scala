package io.tictactoe.authentication.events

import cats.effect.Sync
import io.tictactoe.base.uuid.UUIDGenerator
import io.tictactoe.calendar.Calendar
import io.tictactoe.events.bus.Event
import io.tictactoe.values._
import cats.implicits._

final case class PasswordChangedEvent(
    override val eventId: EventId,
    override val eventTimestamp: EventTimestamp,
    userId: UserId
) extends Event

object PasswordChangedEvent {

  def create[F[_]: Sync: Calendar: UUIDGenerator](userId: UserId): F[Event] =
    for {
      timestamp <- Calendar[F].now()
      id <- EventId.next[F]
    } yield PasswordChangedEvent(id, EventTimestamp(timestamp), userId)

}
