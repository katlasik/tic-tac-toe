package io.tictactoe.authentication.infrastructure.services

import io.tictactoe.authentication.model.Credentials
import io.tictactoe.utilities.authentication.Authentication.JWTToken

trait Authenticator[F[_]] {
  def authenticate(credentials: Credentials): F[JWTToken]
}
