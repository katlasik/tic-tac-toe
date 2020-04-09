package io.tictactoe.game.model

import io.tictactoe.game.values.GameId
import io.tictactoe.values.UserId

final case class InvitationResult(gameId: GameId, guestId: Option[UserId])
