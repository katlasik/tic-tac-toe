package io.tictactoe.calendar

import java.time.Instant

import cats.effect.Sync

trait Calendar[F[_]] {
  def now(): F[Instant]
}

object Calendar {

  def apply[F[_]](implicit ev: Calendar[F]): Calendar[F] = ev

  def live[F[_]: Sync]: Calendar[F] = () => Sync[F].delay(Instant.now())

}
