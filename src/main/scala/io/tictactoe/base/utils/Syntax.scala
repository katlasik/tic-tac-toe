package io.tictactoe.base.utils

import cats.effect.Sync
import cats.implicits._

object Syntax {

  implicit class OptionFSyntax[F[_]: Sync, A](v: F[Option[A]]) {
    def throwIfEmpty[E <: Throwable](throwable: E): F[A] = throwIfEmptyFilter(throwable)(_ => true)

    def throwIfEmptyFilter[E <: Throwable](throwable: E)(predicate: A => Boolean): F[A] =
      v.flatMap(o => Sync[F].fromOption(o.filter(predicate), throwable))
  }

}
