package io.tictactoe.infrastructure.events.error

import io.tictactoe.error.BaseError
import io.tictactoe.infrastructure.events.model.Event

final case class FullEventBusError(event: Event) extends BaseError(s"Event queue is full. Dropping event: $event.")
