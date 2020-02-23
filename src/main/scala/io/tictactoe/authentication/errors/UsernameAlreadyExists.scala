package io.tictactoe.authentication.errors

import io.tictactoe.base.validation.ValidationError

case object UsernameAlreadyExists extends ValidationError("Username already exists.")
