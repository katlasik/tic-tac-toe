package io.tictactoe.authentication.domain.services

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.authentication.model.{Credentials, TokenPayload}
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.authentication.errors.{AccountNotConfirmed, WrongCredentials}
import io.tictactoe.authentication.infrastructure.effects.PasswordHasher
import io.tictactoe.authentication.infrastructure.repositories.AuthRepository
import io.tictactoe.authentication.infrastructure.services.Authenticator
import io.tictactoe.utilities.authentication.Authentication
import io.tictactoe.utilities.authentication.Authentication.JWTToken
import io.tictactoe.utilities.syntax._
import io.tictactoe.values.Confirmed

object LiveAuthenticator {

  def live[F[_]: PasswordHasher: Sync: Logging: Authentication](authRepository: AuthRepository[F]): F[Authenticator[F]] =
    for {
      logger <- Logging[F].create[Authenticator[F]]
    } yield
      new Authenticator[F] {

        override def authenticate(credentials: Credentials): F[JWTToken] =
          for {
            user <- authRepository
              .getByEmail(credentials.email)
              .throwIfEmpty(WrongCredentials)
              .ensure(AccountNotConfirmed)(_.isConfirmed === Confirmed)
            token <- Sync[F].ifM(PasswordHasher[F].check(credentials.password, user.hash))(
              Authentication[F].create(TokenPayload.fromUser(user)),
              logger.info(show"Unsuccessful login attempt for user with id = ${user.id}.") >> Sync[F].raiseError(WrongCredentials)
            )
            _ <- logger.info(show"User with id = ${user.id} authenticated.")
          } yield token
      }
}
