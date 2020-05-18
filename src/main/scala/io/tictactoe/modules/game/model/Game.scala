package io.tictactoe.modules.game.model


import io.tictactoe.values.{GameId, UserId}

final case class Game(gameId: GameId, hostId: UserId, guestId: UserId, initialPlayerId: UserId)
