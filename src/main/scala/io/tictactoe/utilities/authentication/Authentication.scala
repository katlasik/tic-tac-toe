package io.tictactoe.utilities.authentication

import cats.effect.Sync
import io.tictactoe.utilities.authentication.Authentication.JWTToken
import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration._
import io.circe.generic.auto._
import cats.implicits._
import io.tictactoe.utilities.authentication.model.TokenPayload

trait Authentication[F[_]] {
  def verify(token: String): F[TokenPayload]

  def create(tokenPayload: TokenPayload): F[JWTToken]
}

object Authentication {

  type JWTToken = AugmentedJWT[HMACSHA256, TokenPayload]

  def apply[F[_]](implicit ev: Authentication[F]): Authentication[F] = ev

  def live[F[_]: Sync]: F[Authentication[F]] = for {
    key <- HMACSHA256.generateKey[F]
    authenticator = JWTAuthenticator.pstateless.inBearerToken[F, TokenPayload, HMACSHA256](
      expiryDuration = 1.hour,
      maxIdle = None,
      signingKey = key
    )
  } yield new Authentication[F] {
    def verify(token: String): F[TokenPayload] =
      for {
        parsed <- JWTMac.verifyAndParse[F, HMACSHA256](token, key)
        view <- parsed.body.asF[F, TokenPayload]
      } yield view

    override def create(tokenPayload: TokenPayload): F[JWTToken] = authenticator.create(tokenPayload)

  }

}
