package io.tictactoe.game.infrastructure.services

import io.tictactoe.game.model.GameInvitation
import io.tictactoe.values.{Email, GameId, UserId}

trait GameInvitationService[F[_]] {
  def acceptInvitation(gameId: GameId, inviteeId: UserId): F[GameInvitation]

  def inviteByEmail(inviterId: UserId, inviteeEmail: Email): F[GameInvitation]

  def inviteById(inviterId: UserId, inviteeId: UserId): F[GameInvitation]

  def rejectInvitation(gameId: GameId, inviteeId: UserId): F[GameInvitation]

  def get(gameId: GameId): F[Option[GameInvitation]]

  def acceptInvitationAndSetInvitee(gameId: GameId, inviteeId: UserId): F[GameInvitation]
}
