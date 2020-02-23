package io.tictactoe.authentication.model

import cats.effect.Sync
import io.tictactoe.base.validation.Validator
import cats.implicits._
import io.tictactoe.values.{Email, Password, Username}

final case class RegistrationRequest(
    username: String,
    password: String,
    email: String
)

final case class ValidatedRegistrationRequest(
    username: Username,
    password: Password,
    email: Email
)

object RegistrationRequest {

  implicit val validator: Validator[RegistrationRequest, ValidatedRegistrationRequest] =
    new Validator[RegistrationRequest, ValidatedRegistrationRequest] {
      override def validate[F[_]: Sync](request: RegistrationRequest): F[ValidatedRegistrationRequest] = {
        Sync[F].fromValidated(
          (Username.fromString(request.username), Password.fromString(request.password), Email.fromString(request.email))
            .mapN((username, password, email) => ValidatedRegistrationRequest(username, password, email))
            .leftMap(_.reduce)
        )
      }
    }
}
