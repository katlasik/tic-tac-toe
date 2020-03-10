package io.tictactoe.users.repositories

import cats.effect.Sync
import io.tictactoe.authentication.services.PasswordHasher
import io.tictactoe.database.Database
import io.tictactoe.users.model.{DetailedUser, SimpleUser}
import doobie.implicits._
import doobie.postgres.implicits._
import io.tictactoe.values.UserId

trait UserRepository[F[_]] {

  def confirmedUsers(): F[List[SimpleUser]]

  def getById(id: UserId): F[Option[DetailedUser]]

}

object UserRepository {
  def apply[F[_]](implicit ev: UserRepository[F]): UserRepository[F] = ev

  def postgresql[F[_]: Sync: PasswordHasher: Database]: UserRepository[F] = new UserRepository[F] {

    override def getById(id: UserId): F[Option[DetailedUser]] =
      sql"SELECT id, name, email FROM users WHERE id = $id".query[DetailedUser].option.transact(Database[F].transactor())

    override def confirmedUsers(): F[List[SimpleUser]] =
      sql"SELECT id, name FROM users WHERE is_confirmed = true".query[SimpleUser].to[List].transact(Database[F].transactor())

  }
}
