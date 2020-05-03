package io.tictactoe.utilities.authorization

import cats.Monad
import cats.effect.Sync
import cats.implicits._

trait ResourceAuthorization[F[_], -R, U] {
  def canAccess(resource: R, user: U, claim: Claim): F[Boolean]
}

object ResourceAuthorization {

  def apply[F[_]: Monad, R, U](
      canRead: (R, U) => Boolean,
      canWrite: (R, U) => Boolean
  ): ResourceAuthorization[F, R, U] =
    (resource: R, user: U, claim: Claim) =>
      claim match {
        case Read         => Monad[F].pure(canRead(resource, user))
        case ReadAndWrite => Monad[F].pure(canWrite(resource, user))
    }

  implicit def optionDerivation[F[_]: Sync, R, U](
      implicit ra: ResourceAuthorization[F, R, U]
  ): ResourceAuthorization[F, Option[R], U] = (wrappedResource: Option[R], user: U, claim: Claim) => {
    wrappedResource match {
      case Some(r) => ra.canAccess(r, user, claim)
      case _       => Sync[F].pure(false)
    }
  }

  implicit def listDerivation[F[_]: Sync, R, U](
      implicit ra: ResourceAuthorization[F, R, U]
  ): ResourceAuthorization[F, List[R], U] = (wrappedResource: List[R], user: U, claim: Claim) => {
    wrappedResource.forallM(ra.canAccess(_, user, claim))
  }

}
