package io.tictactoe.modules.game.infrastructure.routes

import cats.data.NonEmptyList
import cats.effect.{ContextShift, Sync}
import cats.implicits._
import io.circe.generic.auto._
import io.tictactoe.errors.ErrorView
import io.tictactoe.modules.game.infrastructure.services.GameInvitationService
import io.tictactoe.modules.game.model.{EmailInvitationRequest, InvitationResult, UserInvitationRequest}
import io.tictactoe.utilities.authentication.Authentication
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.routes.Router
import io.tictactoe.values.{BearerToken, GameId}
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import io.tictactoe.implicits._

class GameRouter[F[_]: Sync: ContextShift: Logging: Authentication](
    invitationService: GameInvitationService[F]
) extends Router[F] {

  override def serverEndpoints: NonEmptyList[ServerEndpoint[_, _, _, Nothing, F]] = {

    val inviteByEmail = endpoint
      .description("Endpoint for inviting unregistered users by email.")
      .post
      .in("games" / "invitation")
      .in(auth.bearer[BearerToken])
      .in(jsonBody[EmailInvitationRequest])
      .out(jsonBody[InvitationResult].description("Details of invitation."))
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .serverLogic {
        case (token, request) =>
          val result = for {
            user <- Authentication[F].verify(token)
            invitation <- invitationService.inviteByEmail(user.id, request.email)
          } yield InvitationResult.fromGameInvitation(invitation)

          result.mapErrors
      }

    val inviteUser = endpoint
      .description("Endpoint for inviting other players.")
      .post
      .in("games")
      .in(auth.bearer[BearerToken])
      .in(jsonBody[UserInvitationRequest])
      .out(jsonBody[InvitationResult].description("Details of invitation."))
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .serverLogic {
        case (token, request) =>
          val result = for {
            user <- Authentication[F].verify(token)
            invitation <- invitationService.inviteById(user.id, request.userId)
          } yield InvitationResult.fromGameInvitation(invitation)

          result.mapErrors
      }

    val acceptInvitation = endpoint
      .description("Endpoint for accepting other players' invitations.")
      .post
      .in("games" / path[GameId])
      .in(auth.bearer[BearerToken])
      .out(jsonBody[InvitationResult].description("Details of invitation."))
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .serverLogic {
        case (gameId, token) =>
          val result = for {
            user <- Authentication[F].verify(token)
            invitation <- invitationService.acceptInvitation(gameId, user.id)
          } yield InvitationResult.fromGameInvitation(invitation)

          result.mapErrors
      }

    val rejectInvitation = endpoint
      .description("Endpoint for cancelling player's own invitations/rejecting other players' invitations.")
      .delete
      .in("games" / path[GameId])
      .in(auth.bearer[BearerToken])
      .out(jsonBody[InvitationResult].description("Details of invitation."))
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .serverLogic {
        case (gameId, token) =>
          val result = for {
            user <- Authentication[F].verify(token)
            invitation <- invitationService.rejectInvitation(gameId, user.id)
          } yield InvitationResult.fromGameInvitation(invitation)

          result.mapErrors
      }

    NonEmptyList.of(inviteByEmail, inviteUser, acceptInvitation, rejectInvitation)
  }

}
