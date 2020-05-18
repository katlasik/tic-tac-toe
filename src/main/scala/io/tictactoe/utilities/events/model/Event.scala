package io.tictactoe.utilities.events.model

import cats.Order
import io.tictactoe.values.{EventId, EventTimestamp}
import io.tictactoe.implicits._
import io.chrisdavenport.cats.time._

trait Event {
  val eventId: EventId
  val eventTimestamp: EventTimestamp
}

object Event {

  implicit val order: Order[Event] = Order.by[Event, EventTimestamp](_.eventTimestamp)

}
