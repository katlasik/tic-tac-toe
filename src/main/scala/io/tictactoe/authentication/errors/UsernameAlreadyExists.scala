package io.tictactoe.authentication.errors

import io.tictactoe.utilities.validation.ValidationError

case object UsernameAlreadyExists extends ValidationError("Username already exists.")
