package io.tictactoe.authentication.errors

import io.tictactoe.base.validation.ValidationError

case object EmailAlreadyExists extends ValidationError("Email already exists.")
