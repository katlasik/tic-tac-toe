package io.tictactoe.modules.game.errors

import io.tictactoe.errors.BaseError

final case object InviteSelfError extends BaseError("Can't invite self.")
