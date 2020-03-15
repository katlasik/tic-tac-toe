package io.tictactoe.authentication.model

import io.tictactoe.authentication.services.Hash
import io.tictactoe.authentication.values.ConfirmationToken
import io.tictactoe.values.{Email, IsConfirmed, UserId, Username}

final case class User(
    id: UserId,
    username: Username,
    hash: Hash,
    email: Email,
    isConfirmed: IsConfirmed,
    confirmationToken: Option[ConfirmationToken]
)
