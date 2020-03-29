package io.tictactoe.authentication.errors

import io.tictactoe.infrastructure.validation.ValidationError

case object UsernameAlreadyExists extends ValidationError("Username already exists.")
