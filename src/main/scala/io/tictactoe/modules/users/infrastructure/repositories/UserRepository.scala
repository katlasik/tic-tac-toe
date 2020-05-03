package io.tictactoe.modules.users.infrastructure.repositories

import cats.effect.Sync
import io.tictactoe.utilities.database.Database
import io.tictactoe.modules.users.model.{DetailedUser, SimpleUser}
import doobie.implicits._
import doobie.postgres.implicits._
import io.tictactoe.values.{Email, UserId}

trait UserRepository[F[_]] {
  def getByEmail(email: Email): F[Option[DetailedUser]]

  def confirmedUsers(): F[List[SimpleUser]]

  def getById(id: UserId): F[Option[DetailedUser]]

}

object UserRepository {
  def apply[F[_]](implicit ev: UserRepository[F]): UserRepository[F] = ev

  def postgresql[F[_]: Sync: Database]: UserRepository[F] = new UserRepository[F] {

    val transactor = Database[F].transactor()

    override def getByEmail(email: Email): F[Option[DetailedUser]] =
      sql"SELECT id, name, email FROM users WHERE email = $email".query[DetailedUser].option.transact(transactor)

    override def getById(id: UserId): F[Option[DetailedUser]] =
      sql"SELECT id, name, email FROM users WHERE id = $id".query[DetailedUser].option.transact(transactor)

    override def confirmedUsers(): F[List[SimpleUser]] =
      sql"SELECT id, name FROM users WHERE is_confirmed = true".query[SimpleUser].to[List].transact(transactor)

  }
}
