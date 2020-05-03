package io.tictactoe.modules.authentication.errors

import io.tictactoe.utilities.validation.ValidationError

case object EmailAlreadyExists extends ValidationError("Email already exists.")
