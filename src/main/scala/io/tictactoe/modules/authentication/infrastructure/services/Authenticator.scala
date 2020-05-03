package io.tictactoe.modules.authentication.infrastructure.services

import io.tictactoe.modules.authentication.model.Credentials
import io.tictactoe.utilities.authentication.Authentication.JWTToken

trait Authenticator[F[_]] {
  def authenticate(credentials: Credentials): F[JWTToken]
}
