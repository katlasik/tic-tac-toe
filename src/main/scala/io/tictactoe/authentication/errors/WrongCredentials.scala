package io.tictactoe.authentication.errors

import io.tictactoe.error.BaseError

case object WrongCredentials extends BaseError("Invalid credentials.")
