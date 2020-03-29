package io.tictactoe.authentication.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres.implicits._
import io.tictactoe.authentication.model.User
import io.tictactoe.authentication.services.{Hash, PasswordHasher}
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, UserId, Username, Yes}
import io.tictactoe.database.Database

trait AuthRepository[F[_]] {
  def updateRegistrationConfirmationToken(userId: UserId, token: ConfirmationToken): F[Unit]

  def updatePasswordResetToken(userId: UserId, token: ConfirmationToken): F[Unit]

  def updateHash(userId: UserId, hash: Hash): F[Unit]

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

    val transactor: HikariTransactor[F] = Database[F].transactor()

    override def existsByEmail(email: Email): F[Boolean] =
      sql"SELECT 1 FROM users WHERE email = $email".query[Boolean].option.transact(transactor).map(_.isDefined)

    override def existsByName(username: Username): F[Boolean] =
      sql"SELECT 1 FROM users WHERE name = $username".query[Boolean].option.transact(transactor).map(_.isDefined)

    override def save(user: User): F[User] = user match {
      case User(id, name, hash, email, isConfirmed, confirmationToken, _) =>
        for {
          _ <- sql"INSERT INTO users(id, name, hash, email, is_confirmed, confirmation_token) VALUES ($id, $name, $hash, $email, $isConfirmed, $confirmationToken)".update.run
            .transact(transactor)
        } yield user
    }

    override def confirm(user: User): F[User] =
      sql"UPDATE users SET is_confirmed = true, confirmation_token = null WHERE id = ${user.id}".update.run
        .transact(transactor) *> Sync[F].pure(user.copy(isConfirmed = Yes, registrationConfirmationToken = None))

    override def getByEmail(email: Email): F[Option[User]] =
      sql"SELECT id, name, hash, email, is_confirmed, updated_on, password_reset_token FROM users WHERE email = $email"
        .query[User]
        .option
        .transact(transactor)

    override def getById(id: UserId): F[Option[User]] =
      sql"SELECT id, name, hash, email, is_confirmed, confirmation_token, password_reset_token FROM users WHERE id = $id"
        .query[User]
        .option
        .transact(transactor)

    override def updateRegistrationConfirmationToken(userId: UserId, token: ConfirmationToken): F[Unit] =
      sql"UPDATE users SET confirmation_token = $token WHERE id = $userId".update.run.transact(transactor).void

    override def updatePasswordResetToken(userId: UserId, token: ConfirmationToken): F[Unit] =
      sql"UPDATE users SET password_reset_token = $token WHERE id = $userId".update.run.transact(transactor).void

    override def updateHash(userId: UserId, hash: Hash): F[Unit] =
      sql"UPDATE users SET password_reset_token = null, hash = $hash WHERE id = $userId".update.run.transact(transactor).void

  }

}
