package io.tictactoe.values

import java.util.UUID

import cats.Eq
import io.tictactoe.utilities.uuid.Id

final case class EventId(value: UUID) extends AnyVal

object EventId extends Id[EventId] {
  implicit val eq: Eq[EventId] = Eq.fromUniversalEquals
}
