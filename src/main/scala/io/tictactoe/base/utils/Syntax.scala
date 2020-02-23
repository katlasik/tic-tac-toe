package io.tictactoe.base.utils

import cats.effect.Sync
import cats.implicits._

object Syntax {

  implicit class OptionFSyntax[F[_]: Sync, A](v: F[Option[A]]) {
    def throwIfEmpty[E <: Throwable](throwable: E): F[A] = v.flatMap(Sync[F].fromOption(_, throwable))
  }

}
