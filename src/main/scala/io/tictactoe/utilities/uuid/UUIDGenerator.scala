package io.tictactoe.utilities.uuid

import java.util.UUID

import cats.effect.Sync

trait UUIDGenerator[F[_]] {
  def next(): F[UUID]
}

object UUIDGenerator {

  def apply[F[_]](implicit ev: UUIDGenerator[F]): UUIDGenerator[F] = ev

  def live[F[_]: Sync]: UUIDGenerator[F] = () => Sync[F].delay(UUID.randomUUID())

}
