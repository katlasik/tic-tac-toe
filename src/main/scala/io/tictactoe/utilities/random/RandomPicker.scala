package io.tictactoe.utilities.random

import cats.Functor
import cats.implicits._

trait RandomPicker[F[_]] {

  def pickOne[A](a: A*): F[A]

}

object RandomPicker {

  def apply[F[_]](implicit ev: RandomPicker[F]): RandomPicker[F] = ev

  def live[F[_]: RandomInt: Functor]: RandomPicker[F] = new RandomPicker[F] {
    override def pickOne[A](a: A*): F[A] = for {
      idx <- RandomInt[F].int(a.size)
    } yield a(idx)
  }
}
