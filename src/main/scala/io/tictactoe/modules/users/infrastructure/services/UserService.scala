package io.tictactoe.modules.users.infrastructure.services

import io.tictactoe.modules.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.values.{Email, UserId}

trait UserService[F[_]] {
  def getByEmail(email: Email): F[Option[DetailedUser]]

  def confirmedUsers(): F[List[SimpleUser]]

  def get(id: UserId): F[Option[DetailedUser]]

}
