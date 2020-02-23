package io.tictactoe.authentication.model

import io.tictactoe.values.{UserId, Username}

final case class TokenPayload(
    id: UserId,
    username: Username
)

object TokenPayload {

  def fromUser(user: User): TokenPayload = TokenPayload(user.id, user.username)

}
