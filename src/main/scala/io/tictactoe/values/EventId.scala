package io.tictactoe.values

import java.util.UUID

import cats.{Eq, Functor}
import io.tictactoe.base.uuid.UUIDGenerator
import cats.implicits._

final case class EventId(value: UUID) extends AnyVal

object EventId {
  implicit val eq: Eq[EventId] = Eq.fromUniversalEquals

  def next[F[_]: UUIDGenerator: Functor]: F[EventId] =
    for {
      id <- UUIDGenerator[F].next()
    } yield EventId(id)
}
