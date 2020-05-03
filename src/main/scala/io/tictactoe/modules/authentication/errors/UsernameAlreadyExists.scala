package io.tictactoe.modules.authentication.errors

import io.tictactoe.utilities.validation.ValidationError

case object UsernameAlreadyExists extends ValidationError("Username already exists.")
