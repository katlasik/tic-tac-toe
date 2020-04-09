package io.tictactoe.users.services

import cats.effect.Sync
import io.tictactoe.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.users.repositories.UserRepository
import io.tictactoe.values.{Email, UserId}

trait UserService[F[_]] {
  def getByEmail(email: Email): F[Option[DetailedUser]]

  def confirmedUsers(): F[List[SimpleUser]]

  def get(id: UserId): F[Option[DetailedUser]]

}

object UserService {
  def apply[F[_]](implicit ev: UserService[F]): UserService[F] = ev

  def live[F[_]: Sync: UserRepository]: UserService[F] = new UserService[F] {

    override def get(id: UserId): F[Option[DetailedUser]] = UserRepository[F].getById(id)

    override def confirmedUsers(): F[List[SimpleUser]] = UserRepository[F].confirmedUsers()

    override def getByEmail(email: Email): F[Option[DetailedUser]] = UserRepository[F].getByEmail(email)
  }
}
