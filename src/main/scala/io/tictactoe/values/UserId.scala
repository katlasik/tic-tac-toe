package io.tictactoe.values

import java.util.UUID

import io.tictactoe.utilities.uuid.Id

final case class UserId(value: UUID) extends AnyVal

object UserId extends Id[UserId]
