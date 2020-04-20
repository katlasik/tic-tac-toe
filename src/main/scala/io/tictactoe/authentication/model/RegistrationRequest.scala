package io.tictactoe.authentication.model

import cats.data.Validated
import cats.effect.Sync
import io.tictactoe.infrastructure.validation.{ValidationError, Validator}
import cats.implicits._
import io.tictactoe.game.values.GameId
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, Password, Username}

final case class RegistrationRequest(
    username: String,
    password: String,
    email: String,
    invitationToken: Option[ConfirmationToken],
    gameId: Option[GameId]
)

final case class ValidatedRegistrationRequest(
    username: Username,
    password: Password,
    email: Email,
    invitationData: Option[(ConfirmationToken, GameId)]
)

object RegistrationRequest {

  implicit val validator: Validator[RegistrationRequest, ValidatedRegistrationRequest] =
    new Validator[RegistrationRequest, ValidatedRegistrationRequest] {

      def validateInvitation(request: RegistrationRequest): Validated[ValidationError, Option[(ConfirmationToken, GameId)]] =
        (request.invitationToken, request.gameId) match {
          case (Some(token), Some(id)) => Some((token, id)).valid
          case (None, None)            => None.valid
          case _                       => ValidationError("Both invitationToken and gameId have to be provided or neither.").invalid
        }

      def validatePayload(request: RegistrationRequest): Validated[ValidationError, ValidatedRegistrationRequest] = {
        validateInvitation(request).andThen { invitationData =>
          (Username.fromString(request.username), Password.fromString(request.password), Email.fromString(request.email))
            .mapN((username, password, email) => ValidatedRegistrationRequest(username, password, email, invitationData))
            .leftMap(_.reduce)
        }
      }

      override def validate[F[_]: Sync](request: RegistrationRequest): F[ValidatedRegistrationRequest] =
        Sync[F].fromValidated(validatePayload(request))
    }
}
