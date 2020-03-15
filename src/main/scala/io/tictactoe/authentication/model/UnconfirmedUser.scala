package io.tictactoe.authentication.model

import io.tictactoe.authentication.values.ConfirmationToken
import io.tictactoe.values.{Email, UserId, Username}

final case class UnconfirmedUser(
    id: UserId,
    username: Username,
    email: Email,
    confirmationToken: ConfirmationToken
)
