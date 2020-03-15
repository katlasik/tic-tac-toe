package io.tictactoe.authentication.repositories

import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.postgres.implicits._
import io.tictactoe.authentication.model.{UnconfirmedUser, User}
import io.tictactoe.authentication.services.PasswordHasher
import io.tictactoe.authentication.values.ConfirmationToken
import io.tictactoe.values.{Email, UserId, Username, Yes}
import io.tictactoe.database.Database


trait AuthRepository[F[_]] {
  def updateToken(userId: UserId, token: ConfirmationToken): F[Unit]

  def confirm(user: User): F[User]

  def addConfirmationEmail(userId: UserId, token: ConfirmationToken): F[Unit]

  def getUsersWithMissingConfirmationEmails(): F[List[UnconfirmedUser]]

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
      sql"SELECT id, name, hash, email, is_confirmed, confirmation_token, updated_on FROM users WHERE email = $email"
        .query[User]
        .option
        .transact(Database[F].transactor())

    override def getById(id: UserId): F[Option[User]] =
      sql"SELECT id, name, hash, email, is_confirmed, confirmation_token FROM users WHERE id = $id"
        .query[User]
        .option
        .transact(Database[F].transactor())

    override def updateToken(userId: UserId, token: ConfirmationToken): F[Unit] =
      sql"UPDATE users SET confirmation_token = $token WHERE id = $userId".update.run.transact(Database[F].transactor()).void

    override def addConfirmationEmail(userId: UserId, token: ConfirmationToken): F[Unit] =
      sql"INSERT INTO users_confirmation_emails(user_id, confirmation_token) VALUES($userId, $token)".update.run.transact(Database[F].transactor()).void

    override def getUsersWithMissingConfirmationEmails(): F[List[UnconfirmedUser]] =
      sql"""SELECT id, name, email, confirmation_token
           |FROM users
           |WHERE is_confirmed = false
           |  AND (updated_on IS NULL OR updated_on < NOW() - interval '15' second)
           |  AND NOT EXISTS(
           |        SELECT 1
           |        FROM users_confirmation_emails
           |        WHERE users_confirmation_emails.user_id = users.id
           |          AND users_confirmation_emails.confirmation_token = users.confirmation_token
           |    )
           |""".stripMargin.query[UnconfirmedUser].to[List].transact(Database[F].transactor())
  }

}
