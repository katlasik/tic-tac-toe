package io.tictactoe.modules.game.infrastructure.services

import io.tictactoe.values.{GameId, UserId}

trait GameService[F[_]] {
  def createGame(gameId: GameId, hostId: UserId, guestIt: UserId): F[Unit]
}
