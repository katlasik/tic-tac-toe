package io.tictactoe.events.model.authentication

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.modules.authentication.model.User
import io.tictactoe.errors.ResourceNotFound
import io.tictactoe.utilities.calendar.Calendar
import io.tictactoe.utilities.events.model.Event
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.utilities.uuid.UUIDGenerator
import io.tictactoe.values._

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
          UserRegisteredEvent(id, EventTimestamp(timestamp), user.id, user.username, user.email, token.some, Unconfirmed).pure[F]
        case None if user.isConfirmed === Confirmed =>
          UserRegisteredEvent(id, EventTimestamp(timestamp), user.id, user.username, user.email, None, Confirmed).pure[F]
        case _ => Sync[F].raiseError(ResourceNotFound)
      }
    } yield result

}
