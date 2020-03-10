package io.tictactoe.authentication.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.postgres.implicits._
import io.tictactoe.authentication.model.User
import io.tictactoe.authentication.services.PasswordHasher
import io.tictactoe.values.{Email, UserId, Username, Yes}
import io.tictactoe.database.Database

trait AuthRepository[F[_]] {
  def confirm(user: User): F[User]

  def getById(id: UserId): F[Option[User]]

  def getByEmail(email: Email): F[Option[User]]

  def existsByEmail(email: Email): F[Boolean]

  def existsByName(username: Username): F[Boolean]

  def save(user: User): F[User]

}

object AuthRepository {

  def apply[F[_]](implicit ev: AuthRepository[F]): AuthRepository[F] = ev

  def postgresql[F[_]: Sync: PasswordHasher: Database]: AuthRepository[F] = new AuthRepository[F] {

    override def existsByEmail(email: Email): F[Boolean] =
      sql"SELECT 1 FROM users WHERE email = $email".query[Boolean].option.transact(Database[F].transactor()).map(_.isDefined)

    override def existsByName(username: Username): F[Boolean] =
      sql"SELECT 1 FROM users WHERE name = $username".query[Boolean].option.transact(Database[F].transactor()).map(_.isDefined)

    override def save(user: User): F[User] = user match {
      case User(id, name, hash, email, isConfirmed, confirmationToken) =>
        for {
          _ <- sql"INSERT INTO users(id, name, hash, email, is_confirmed, confirmation_token) VALUES ($id, $name, $hash, $email, $isConfirmed, $confirmationToken)".update.run
            .transact(Database[F].transactor())
        } yield user
    }

    override def confirm(user: User): F[User] =
      sql"UPDATE users SET is_confirmed = true, confirmation_token = null WHERE id = ${user.id}".update.run
        .transact(Database[F].transactor()) *> Sync[F].pure(user.copy(isConfirmed = Yes, confirmationToken = None))

    override def getByEmail(email: Email): F[Option[User]] =
      sql"SELECT id, name, hash, email, is_confirmed, confirmation_token FROM users WHERE email = $email"
        .query[User]
        .option
        .transact(Database[F].transactor())

    override def getById(id: UserId): F[Option[User]] =
      sql"SELECT id, name, hash, email, is_confirmed, confirmation_token FROM users WHERE id = $id"
        .query[User]
        .option
        .transact(Database[F].transactor())
  }

}
