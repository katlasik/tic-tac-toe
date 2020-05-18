package io.tictactoe.values

import java.util.UUID

import io.tictactoe.utilities.uuid.Id

final case class GameId(value: UUID) extends AnyVal

object GameId extends Id[GameId]
