package io.tictactoe.authentication.events

import cats.effect.Sync
import io.tictactoe.authentication.model.User
import io.tictactoe.infrastructure.uuid.UUIDGenerator
import io.tictactoe.infrastructure.calendar.Calendar
import io.tictactoe.infrastructure.events.model.Event
import io.tictactoe.values.{Confirmed, Email, EventId, EventTimestamp, IsConfirmed, Unconfirmed, UserId, Username}
import cats.implicits._
import io.tictactoe.authentication.errors.ResourceNotFound
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken

final case class UserRegisteredEvent(
    override val eventId: EventId,
    override val eventTimestamp: EventTimestamp,
    userId: UserId,
    username: Username,
    email: Email,
    confirmationToken: Option[ConfirmationToken],
    isConfirmed: IsConfirmed
) extends Event

object UserRegisteredEvent {

  def create[F[_]: Sync: Calendar: UUIDGenerator](user: User): F[Event] =
    for {
      timestamp <- Calendar[F].now()
      id <- EventId.next[F]
      result <- user.registrationConfirmationToken match {
        case Some(token) if user.isConfirmed === Unconfirmed =>
          Sync[F].pure(UserRegisteredEvent(id, EventTimestamp(timestamp), user.id, user.username, user.email, token.some, Unconfirmed))
        case None if user.isConfirmed === Confirmed =>
          Sync[F].pure(UserRegisteredEvent(id, EventTimestamp(timestamp), user.id, user.username, user.email, None, Confirmed))
        case _ => Sync[F].raiseError(ResourceNotFound)
      }
    } yield result

}
