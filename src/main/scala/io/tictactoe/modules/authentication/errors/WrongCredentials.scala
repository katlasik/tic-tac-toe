package io.tictactoe.modules.authentication.errors

import io.tictactoe.errors.BaseError

case object WrongCredentials extends BaseError("Invalid credentials.")
