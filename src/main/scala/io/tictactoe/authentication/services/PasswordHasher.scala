package io.tictactoe.authentication.services

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.values.Password
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

trait PasswordHasher[F[_]] {
  def hash(password: Password): F[Hash]
  def check(password: Password, hash: Hash): F[Boolean]
}

final case class Hash(value: String) extends AnyVal

object PasswordHasher {

  implicit def apply[F[_]](implicit ev: PasswordHasher[F]): PasswordHasher[F] = ev

  def bcrypt[F[_]: Sync]: PasswordHasher[F] = new PasswordHasher[F] {
    private val algorithm = BCrypt.syncPasswordHasher[F]

    override def hash(password: Password): F[Hash] = algorithm.hashpw(password.value).map(p => Hash(p.toString))

    override def check(password: Password, hash: Hash): F[Boolean] =
      algorithm.checkpwBool(password.value, PasswordHash[BCrypt](hash.value))
  }

}
