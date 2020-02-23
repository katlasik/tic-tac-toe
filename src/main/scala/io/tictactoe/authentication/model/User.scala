package io.tictactoe.authentication.model

import io.tictactoe.authentication.services.Hash
import io.tictactoe.values.{Email, UserId, Username}

final case class User(
    id: UserId,
    username: Username,
    hash: Hash,
    email: Email
)
