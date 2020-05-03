package io.tictactoe.modules.users.model

import io.tictactoe.values.{UserId, Username}

final case class SimpleUser(
    id: UserId,
    username: Username
)
