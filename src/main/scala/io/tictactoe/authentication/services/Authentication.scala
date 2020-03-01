package io.tictactoe.authentication.services

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.authentication.model.{Credentials, TokenPayload}
import io.tictactoe.authentication.services.Authentication.JWTToken
import io.tictactoe.base.logging.Logging
import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration._
import io.circe.generic.auto._
import io.tictactoe.authentication.errors.WrongCredentials
import io.tictactoe.authentication.repositories.AuthRepository
import tsec.jws.mac.JWTMac
import io.tictactoe.base.utils.Syntax._

trait Authentication[F[_]] {
  def authenticate(credentials: Credentials): F[JWTToken]

  def verify(token: String): F[TokenPayload]
}

object Authentication {

  type JWTToken = AugmentedJWT[HMACSHA256, TokenPayload]

  def apply[F[_]](implicit ev: Authentication[F]): Authentication[F] = ev

  def live[F[_]: PasswordHasher: AuthRepository: Sync: Logging]: F[Authentication[F]] =
    for {
      logger <- Logging[F].create[Authentication[F]]
      key <- HMACSHA256.generateKey[F]
      authenticator = JWTAuthenticator.pstateless.inBearerToken[F, TokenPayload, HMACSHA256](
        expiryDuration = 1.hour,
        maxIdle = None,
        signingKey = key
      )
    } yield
      new Authentication[F] {

        override def authenticate(credentials: Credentials): F[JWTToken] =
          for {
            user <- AuthRepository[F].getByEmail(credentials.email).throwIfEmpty(WrongCredentials)
            authenticated <- PasswordHasher[F].check(credentials.password, user.hash)
            token <- if (authenticated) {
              logger.info(show"User with id = ${user.id} authenticated.") *> authenticator.create(TokenPayload.fromUser(user))
            } else {
              Sync[F].raiseError(WrongCredentials) <* logger.info(show"Unsuccessful login attempt for user with id = ${user.id}.")
            }
          } yield token

        def verify(token: String): F[TokenPayload] =
          for {
            parsed <- JWTMac.verifyAndParse[F, HMACSHA256](token, key)
            view <- parsed.body.asF[F, TokenPayload]
          } yield view
      }
}
