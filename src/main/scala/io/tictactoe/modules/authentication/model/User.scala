package io.tictactoe.modules.authentication.model

import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, Hash, IsConfirmed, UserId, Username}

final case class User(
    id: UserId,
    username: Username,
    hash: Hash,
    email: Email,
    isConfirmed: IsConfirmed,
    registrationConfirmationToken: Option[ConfirmationToken],
    passwordResetToken: Option[ConfirmationToken]
)
