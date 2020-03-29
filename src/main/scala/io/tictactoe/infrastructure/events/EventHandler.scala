package io.tictactoe.infrastructure.events

import io.tictactoe.infrastructure.events.model.Event

trait EventHandler[F[_]] {
  def handle(event: Event): F[Unit]
}
