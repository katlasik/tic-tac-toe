package io.tictactoe.authentication.events

import cats.effect.Sync
import io.tictactoe.authentication.model.User
import io.tictactoe.base.uuid.UUIDGenerator
import io.tictactoe.calendar.Calendar
import io.tictactoe.events.bus.Event
import io.tictactoe.values.{Email, EventId, EventTimestamp, UserId, Username}
import cats.implicits._
import io.tictactoe.authentication.errors.ResourceNotFound
import io.tictactoe.authentication.values.ConfirmationToken

final case class UserRegisteredEvent(
    override val eventId: EventId,
    override val eventTimestamp: EventTimestamp,
    userId: UserId,
    username: Username,
    email: Email,
    confirmationToken: ConfirmationToken
) extends Event

object UserRegisteredEvent {

  def create[F[_]: Sync: Calendar: UUIDGenerator](user: User): F[Event] =
    for {
      timestamp <- Calendar[F].now()
      id <- EventId.next[F]
      result <- user.confirmationToken match {
        case Some(token) => Sync[F].pure(UserRegisteredEvent(id, EventTimestamp(timestamp), user.id, user.username, user.email, token))
        case _ => Sync[F].raiseError(ResourceNotFound)
      }
    } yield result

}
