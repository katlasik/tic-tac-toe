package io.tictactoe.utilities

import cats.effect.Sync
import cats.implicits._

package object syntax {

  implicit class OptionFSyntax[F[_]: Sync, A](v: F[Option[A]]) {
    def throwIfEmpty[E <: Throwable](throwable: E): F[A] = throwIfEmptyFilter(throwable)(_ => true)

    def throwIfEmptyFilter[E <: Throwable](throwable: E)(predicate: A => Boolean): F[A] =
      v.flatMap(o => Sync[F].fromOption(o.filter(predicate), throwable))
  }

  implicit class ThrowableSyntax[F[_]: Sync](throwable: Throwable) {
    def throwWhenM(f: F[Boolean]): F[Unit] =
      for {
        result <- f
        _ <- Sync[F].raiseError(throwable).whenA(result)
      } yield ()

    def throwWhen[A](condition: Boolean): F[Unit] = Sync[F].raiseError(throwable).whenA(condition)
  }

}
