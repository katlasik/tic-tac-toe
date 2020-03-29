package io.tictactoe.infrastructure.events.model

import cats.Order
import io.tictactoe.values.{EventId, EventTimestamp}

trait Event {
  val eventId: EventId
  val eventTimestamp: EventTimestamp
}

object Event {

  implicit val order: Order[Event] = Order.by(_.eventTimestamp)

}
