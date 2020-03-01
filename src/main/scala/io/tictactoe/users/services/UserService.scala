package io.tictactoe.users.services

import cats.effect.Sync
import io.tictactoe.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.users.repositories.UserRepository
import io.tictactoe.values.UserId

trait UserService[F[_]] {

  def all(): F[List[SimpleUser]]

  def get(id: UserId): F[Option[DetailedUser]]

}

object UserService {
  def apply[F[_]](implicit ev: UserService[F]): UserService[F] = ev

  def live[F[_]: Sync: UserRepository]: UserService[F] = new UserService[F] {

    override def get(id: UserId): F[Option[DetailedUser]] = UserRepository[F].getById(id)

    override def all(): F[List[SimpleUser]] = UserRepository[F].all()

  }
}
