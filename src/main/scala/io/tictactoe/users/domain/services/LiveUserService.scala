package io.tictactoe.users.domain.services

import cats.effect.Sync
import io.tictactoe.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.users.infrastructure.repositories.UserRepository
import io.tictactoe.users.infrastructure.services.UserService
import io.tictactoe.values.{Email, UserId}

object LiveUserService {

  def live[F[_]: Sync](userRepository: UserRepository[F]): UserService[F] = new UserService[F] {

    override def get(id: UserId): F[Option[DetailedUser]] = userRepository.getById(id)

    override def confirmedUsers(): F[List[SimpleUser]] = userRepository.confirmedUsers()

    override def getByEmail(email: Email): F[Option[DetailedUser]] = userRepository.getByEmail(email)
  }
}
