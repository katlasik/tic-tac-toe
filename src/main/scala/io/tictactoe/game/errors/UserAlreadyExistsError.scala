package io.tictactoe.game.errors

import io.tictactoe.error.BaseError
import cats.implicits._
import io.tictactoe.values.Email

final case class UserAlreadyExistsError(email: Email) extends BaseError(show"User with email $email is already registered.")
