package io.tictactoe.authentication.errors

import io.tictactoe.infrastructure.validation.ValidationError

case object EmailAlreadyExists extends ValidationError("Email already exists.")
