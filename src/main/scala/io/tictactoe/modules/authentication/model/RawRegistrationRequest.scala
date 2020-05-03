package io.tictactoe.modules.authentication.model

import cats.data.Validated
import cats.effect.Sync
import io.tictactoe.utilities.validation.{ValidationError, Validator}
import cats.implicits._
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, GameId, Password, Username}

final case class RawRegistrationRequest(
    username: String,
    password: String,
    email: String,
    invitationToken: Option[ConfirmationToken],
    gameId: Option[GameId]
)

final case class RegistrationRequest(
    username: Username,
    password: Password,
    email: Email,
    invitationData: Option[(ConfirmationToken, GameId)]
)

object RawRegistrationRequest {

  implicit val validator: Validator[RawRegistrationRequest, RegistrationRequest] =
    new Validator[RawRegistrationRequest, RegistrationRequest] {

      def validateInvitation(request: RawRegistrationRequest): Validated[ValidationError, Option[(ConfirmationToken, GameId)]] =
        (request.invitationToken, request.gameId) match {
          case (Some(token), Some(id)) => Some((token, id)).valid
          case (None, None)            => None.valid
          case _                       => ValidationError("Both invitationToken and gameId have to be provided or neither.").invalid
        }

      def validatePayload(request: RawRegistrationRequest): Validated[ValidationError, RegistrationRequest] = {
        validateInvitation(request).andThen { invitationData =>
          (Username.fromString(request.username), Password.fromString(request.password), Email.fromString(request.email))
            .mapN((username, password, email) => RegistrationRequest(username, password, email, invitationData))
            .leftMap(_.reduce)
        }
      }

      override def validate[F[_]: Sync](request: RawRegistrationRequest): F[RegistrationRequest] =
        Sync[F].fromValidated(validatePayload(request))
    }
}
