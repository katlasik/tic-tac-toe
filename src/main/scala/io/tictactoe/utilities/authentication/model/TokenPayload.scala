package io.tictactoe.utilities.authentication.model

import io.tictactoe.values.{UserId, Username}

final case class TokenPayload(
    id: UserId,
    username: Username
)
