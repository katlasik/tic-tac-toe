package io.tictactoe.authorization

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.authorization.errors.AccessForbidden

object Authorized {

  def canRead[F[_]: Sync, R, U](user: U, resource: R)(
      implicit ra: ResourceAuthorization[F, R, U]
  ): F[R] = checkClaims(user, resource, Read)

  def canReadAndWrite[F[_]: Sync, R, U](user: U, resource: R)(
      implicit ra: ResourceAuthorization[F, R, U]
  ): F[R] = checkClaims(user, resource, ReadAndWrite)

  def checkClaims[F[_]: Sync, R, U](user: U, resource: R, claim: Claim)(
      implicit ra: ResourceAuthorization[F, R, U]
  ): F[R] = {
    for {
      canAccess <- ra.canAccess(resource, user, claim)
      result <- if (canAccess) {
        Sync[F].pure(resource)
      } else {
        Sync[F].raiseError[R](AccessForbidden)
      }
    } yield result
  }

}
