package io.tictactoe.authentication.model

import io.tictactoe.base.tokens.values.ConfirmationToken
import io.tictactoe.values.{Password, UserId}

final case class PasswordChangeRequest(
    id: UserId,
    token: ConfirmationToken,
    password: Password
)