package io.tictactoe.utilities.random

import java.security.SecureRandom

import cats.effect.Sync
import cats.implicits._

trait RandomInt[F[_]] {

  def int(max: Int): F[Int]

}

object RandomInt {

  def apply[F[_]](implicit ev: RandomInt[F]): RandomInt[F] = ev

  def live[F[_]: Sync]: F[RandomInt[F]] = for {
     generator <- Sync[F].delay(new SecureRandom)
  } yield new RandomInt[F] {
    override def int(bound: Int): F[Int] = Sync[F].delay(generator.nextInt(bound))
  }

}
