package io.tictactoe.game.model

import java.time.LocalDateTime

import cats.effect.Sync
import io.tictactoe.utilities.authorization.{Claim, ResourceAuthorization}
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, GameId, UserId}
import cats.implicits._
import io.tictactoe.game.model.GameInvitationStatus._

sealed trait GameInvitation extends Product with Serializable {
  val id: GameId
  val ownerId: UserId
  val token: Option[ConfirmationToken]
  val guestEmail: Option[Email]

  def status: GameInvitationStatus
}

sealed trait UnacceptedGameInvitation extends GameInvitation {
  val guestId: Option[UserId]
}

final case class PendingGameInvitation(
    id: GameId,
    ownerId: UserId,
    guestId: Option[UserId],
    token: Option[ConfirmationToken],
    guestEmail: Option[Email]
) extends UnacceptedGameInvitation {
  override val status: GameInvitationStatus = Pending;
}

final case class AcceptedGameInvitation(
    id: GameId,
    ownerId: UserId,
    guestId: UserId,
    token: Option[ConfirmationToken],
    guestEmail: Option[Email],
    acceptedOn: LocalDateTime
) extends GameInvitation {
  override val status: GameInvitationStatus = Accepted;
}

final case class RejectedGameInvitation(
    id: GameId,
    ownerId: UserId,
    guestId: Option[UserId],
    token: Option[ConfirmationToken],
    guestEmail: Option[Email],
    rejectedOn: LocalDateTime
) extends UnacceptedGameInvitation {
  override val status: GameInvitationStatus = Rejected;
}

final case class CancelledGameInvitation(
    id: GameId,
    ownerId: UserId,
    guestId: Option[UserId],
    token: Option[ConfirmationToken],
    guestEmail: Option[Email],
    cancelledOn: LocalDateTime
) extends UnacceptedGameInvitation {
  override val status: GameInvitationStatus = Cancelled;
}

object GameInvitation {

  def withGuestId(id: GameId, ownerId: UserId, guestId: UserId): PendingGameInvitation =
    PendingGameInvitation(id, ownerId, guestId.some, none, none)

  def withEmail(id: GameId, ownerId: UserId, guestEmail: Email, token: ConfirmationToken): PendingGameInvitation =
    PendingGameInvitation(id, ownerId, none, token.some, guestEmail.some)

  implicit def authorization[F[_]: Sync]: ResourceAuthorization[F, GameInvitation, UserId] =
    (resource: GameInvitation, user: UserId, _: Claim) =>
      resource match {
        case r: PendingGameInvitation => Sync[F].pure(r.ownerId === user || r.guestId.contains(user))
        case _                        => Sync[F].pure(false)
    }

}
