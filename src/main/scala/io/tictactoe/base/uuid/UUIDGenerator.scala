package io.tictactoe.base.uuid

import java.util.UUID

import cats.effect.Sync

trait UUIDGenerator[F[_]] {
  def next(): F[UUID]
}

object UUIDGenerator {

  implicit def apply[F[_]](implicit ev: UUIDGenerator[F]): UUIDGenerator[F] = ev

  def live[F[_]: Sync]: UUIDGenerator[F] = () => Sync[F].delay(UUID.randomUUID())

}
