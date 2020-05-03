package io.tictactoe.modules.game.model

import io.tictactoe.values.{GameId, UserId}
import cats.syntax.option._
import io.tictactoe.modules.game.model.GameInvitationStatus.Accepted

final case class InvitationResult(gameId: GameId, guestId: Option[UserId], hostId: UserId, status: GameInvitationStatus)

object InvitationResult {

  def fromGameInvitation(invitation: GameInvitation): InvitationResult = invitation match {
    case AcceptedGameInvitation(id, ownerId, guestId, _, _, _) =>
      InvitationResult(id, guestId.some, ownerId, Accepted)
    case invitation: UnacceptedGameInvitation =>
      InvitationResult(invitation.id, invitation.guestId, invitation.ownerId, invitation.status)
  }

}
