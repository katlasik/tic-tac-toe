package io.tictactoe.modules.users.model

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.utilities.authentication.model.TokenPayload
import io.tictactoe.values.{Email, UserId, Username}
import io.tictactoe.utilities.authorization.{Claim, ResourceAuthorization}

final case class DetailedUser(
    id: UserId,
    username: Username,
    email: Email
)

object DetailedUser {

  implicit def authorization[F[_]: Sync]: ResourceAuthorization[F, DetailedUser, TokenPayload] =
    (resource: DetailedUser, user: TokenPayload, _: Claim) => Sync[F].pure(resource.id === user.id)

}
