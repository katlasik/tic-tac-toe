package io.tictactoe.authentication.errors

import io.tictactoe.utilities.validation.ValidationError

case object EmailAlreadyExists extends ValidationError("Email already exists.")
