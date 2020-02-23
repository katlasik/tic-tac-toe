package io.tictactoe.values

import java.util.UUID

import cats.Eq

final case class EventId(value: UUID) extends AnyVal

object EventId {
  implicit val eq: Eq[EventId] = Eq.fromUniversalEquals
}
