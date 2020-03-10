package io.tictactoe.values

import java.time.Instant
import io.chrisdavenport.cats.time._

import cats.Order

final case class EventTimestamp(value: Instant) extends AnyVal

object EventTimestamp {

  implicit val order: Order[EventTimestamp] = Order.by(_.value)

}
