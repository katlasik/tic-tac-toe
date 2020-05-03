package io.tictactoe.authentication.errors

import io.tictactoe.errors.BaseError

case object WrongCredentials extends BaseError("Invalid credentials.")
