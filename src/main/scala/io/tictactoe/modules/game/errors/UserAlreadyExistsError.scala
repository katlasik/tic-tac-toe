package io.tictactoe.modules.game.errors

import io.tictactoe.errors.BaseError
import cats.implicits._
import io.tictactoe.values.Email
import io.tictactoe.implicits._

final case class UserAlreadyExistsError(email: Email) extends BaseError(show"User with email $email is already registered.")
