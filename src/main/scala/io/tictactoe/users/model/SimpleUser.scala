package io.tictactoe.users.model

import io.tictactoe.values.{UserId, Username}

final case class SimpleUser(
    id: UserId,
    username: Username
)
