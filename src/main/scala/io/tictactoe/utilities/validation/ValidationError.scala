package io.tictactoe.utilities.validation

import cats.kernel.Semigroup
import io.tictactoe.errors.BaseError

class ValidationError(msg: String) extends BaseError(msg)

object ValidationError {

  def apply(msg: String): ValidationError = new ValidationError(msg)

  implicit val semigroup: Semigroup[ValidationError] = (x, y) => ValidationError(x.msg + ", " + y.msg)

}
