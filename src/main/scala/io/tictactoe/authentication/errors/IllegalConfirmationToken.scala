package io.tictactoe.authentication.errors

import io.tictactoe.utilities.validation.ValidationError

case object IllegalConfirmationToken extends ValidationError("Illegal confirmation token.")
