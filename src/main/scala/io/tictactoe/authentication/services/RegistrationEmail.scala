package io.tictactoe.authentication.services

import cats.effect.Sync
import io.tictactoe.base.logging.Logging
import io.tictactoe.values.{Email, Username}
import cats.implicits._

trait RegistrationEmail[F[_]] {
  def send(email: Email, username: Username): F[Unit]
}

object RegistrationEmail {

  def apply[F[_]](implicit ev: RegistrationEmail[F]): RegistrationEmail[F] = ev

  def live[F[_]: Sync: Logging]: F[RegistrationEmail[F]] =
    for {
      logger <- Logging[F].create[RegistrationEmail.type]
    } yield
      new RegistrationEmail[F] {
        override def send(email: Email, username: Username): F[Unit] = logger.info(show"Sending registration email to $email.")
      }
}
