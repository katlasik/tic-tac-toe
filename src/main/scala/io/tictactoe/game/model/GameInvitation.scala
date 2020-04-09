package io.tictactoe.game.model

import io.tictactoe.game.values.GameId
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, UserId}

final case class GameInvitation(id: GameId, ownerId: UserId, guestId: Option[UserId], token: Option[ConfirmationToken], guestEmail: Option[Email])
