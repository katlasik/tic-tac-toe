package io.tictactoe.authentication.errors

import io.tictactoe.base.validation.ValidationError

case object IllegalConfirmationToken extends ValidationError("Illegal confirmation token.")
