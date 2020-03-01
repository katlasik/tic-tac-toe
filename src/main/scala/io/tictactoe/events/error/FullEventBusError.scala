package io.tictactoe.events.error

import io.tictactoe.error.BaseError
import io.tictactoe.events.bus.Event

final case class FullEventBusError(event: Event) extends BaseError(s"Event queue is full. Dropping event: $event.")
