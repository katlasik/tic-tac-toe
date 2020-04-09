package io.tictactoe.game.errors

import io.tictactoe.error.BaseError

final case object InviteSelfError extends BaseError("Can't invite self.")
