package io.tictactoe.modules.authentication.errors

import io.tictactoe.errors.BaseError

case object AccountNotConfirmed extends BaseError("Account is not yet confirmed.")
