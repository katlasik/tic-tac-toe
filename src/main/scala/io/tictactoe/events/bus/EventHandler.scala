package io.tictactoe.events.bus

trait EventHandler[F[_]] {
  def handle(event: Event): F[Unit]
}
