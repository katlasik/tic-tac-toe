package io.tictactoe.authentication.errors

import io.tictactoe.infrastructure.validation.ValidationError

case object IllegalConfirmationToken extends ValidationError("Illegal confirmation token.")
