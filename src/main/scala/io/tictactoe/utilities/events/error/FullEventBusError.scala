package io.tictactoe.utilities.events.error

import io.tictactoe.errors.BaseError
import io.tictactoe.utilities.events.model.Event

final case class FullEventBusError(event: Event) extends BaseError(s"Event queue is full. Dropping event: $event.")
