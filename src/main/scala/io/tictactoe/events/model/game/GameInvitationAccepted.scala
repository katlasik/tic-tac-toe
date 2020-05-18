package io.tictactoe.events.model.game

import cats.effect.Sync
import io.tictactoe.utilities.calendar.Calendar
import io.tictactoe.utilities.events.model.Event
import io.tictactoe.utilities.uuid.UUIDGenerator
import io.tictactoe.values.{EventId, EventTimestamp, GameId, UserId}
import cats.implicits._

final case class GameInvitationAccepted(
    override val eventId: EventId,
    override val eventTimestamp: EventTimestamp,
    gameId: GameId,
    ownerId: UserId,
    guestId: UserId
) extends Event

object GameInvitationAccepted {

  def create[F[_]: Sync: Calendar: UUIDGenerator](gameId: GameId, host: UserId, guest: UserId): F[GameInvitationAccepted] =
    for {
      timestamp <- Calendar[F].now().map(EventTimestamp(_))
      id <- EventId.next[F]
    } yield GameInvitationAccepted(id, timestamp, gameId, host, guest)

}
