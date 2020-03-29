package io.tictactoe.authentication.services

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.authentication.model.{Credentials, TokenPayload}
import io.tictactoe.authentication.services.Authentication.JWTToken
import io.tictactoe.infrastructure.logging.Logging
import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration._
import io.circe.generic.auto._
import io.tictactoe.authentication.errors.{AccountNotConfirmed, WrongCredentials}
import io.tictactoe.authentication.repositories.AuthRepository
import tsec.jws.mac.JWTMac
import io.tictactoe.infrastructure.utils.Syntax._
import io.tictactoe.values.No

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
            _ <- Sync[F].whenA(user.isConfirmed === No)(Sync[F].raiseError(AccountNotConfirmed))
            token <- Sync[F].ifM(PasswordHasher[F].check(credentials.password, user.hash))(
              authenticator.create(TokenPayload.fromUser(user)),
              logger.info(show"Unsuccessful login attempt for user with id = ${user.id}.") >> Sync[F].raiseError(WrongCredentials)
            )
            _ <- logger.info(show"User with id = ${user.id} authenticated.")
          } yield token

        def verify(token: String): F[TokenPayload] =
          for {
            parsed <- JWTMac.verifyAndParse[F, HMACSHA256](token, key)
            view <- parsed.body.asF[F, TokenPayload]
          } yield view
      }
}
