package io.tictactoe.modules.authentication.api

import io.tictactoe.modules.authentication.model.User
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, Hash, UserId, Username}

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
