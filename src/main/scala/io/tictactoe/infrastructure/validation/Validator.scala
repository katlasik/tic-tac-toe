package io.tictactoe.infrastructure.validation

import cats.effect.Sync

trait Validator[R, O] {
  def validate[F[_]: Sync](resource: R): F[O]
}

object Validator {

  implicit class ValidatorOps[R, O](r: R)(implicit validator: Validator[R, O]) {
    def validate[F[_]: Sync]: F[O] = validator.validate(r)
  }

}
