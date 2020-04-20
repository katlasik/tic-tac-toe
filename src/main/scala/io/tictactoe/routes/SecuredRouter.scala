package io.tictactoe.routes

import cats.effect.{ContextShift, Sync}
import io.tictactoe.authentication.services.{Authentication, Registration}
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._
import io.tictactoe.infrastructure.syntax._
import io.tictactoe.infrastructure.errors.ErrorMapper._
import cats.implicits._
import io.tictactoe.authentication.errors.ResourceNotFound
import io.tictactoe.authorization.Authorized._
import io.tictactoe.game.model.InvitationResult
import io.tictactoe.game.services.GameInvitationService
import io.tictactoe.infrastructure.logging.Logging
import io.tictactoe.users.services.UserService

object SecuredRouter {

  def routes[F[_]: Sync: ContextShift: Registration: Authentication: UserService: Logging: GameInvitationService]: HttpRoutes[F] = {

    val getUsers = Endpoints.getUsers.toRoutes(token => {
      val result = for {
        _ <- Authentication[F].verify(token)
        result <- UserService[F].confirmedUsers()
      } yield result

      result.mapErrors
    })

    val getUser = Endpoints.getUser.toRoutes {
      case (id, token) =>
        val result = for {
          user <- Authentication[F].verify(token)
          resource <- UserService[F].get(id).throwIfEmpty(ResourceNotFound)
          _ <- canRead(user, resource)
        } yield resource

        result.mapErrors
    }

    val invitationByEmail = Endpoints.inviteByEmail.toRoutes {
      case (token, request) =>
        val result = for {
          user <- Authentication[F].verify(token)
          invitation <- GameInvitationService[F].inviteByEmail(user.id, request.email)
        } yield InvitationResult.fromGameInvitation(invitation)

        result.mapErrors
    }

    val userInvitation = Endpoints.inviteUser.toRoutes {
      case (token, request) =>
        val result = for {
          user <- Authentication[F].verify(token)
          invitation <- GameInvitationService[F].inviteById(user.id, request.userId)
        } yield InvitationResult.fromGameInvitation(invitation)

        result.mapErrors
    }

    val acceptInvitation = Endpoints.acceptInvitation.toRoutes {
      case (gameId, token) =>
        val result = for {
          user <- Authentication[F].verify(token)
          invitation <- GameInvitationService[F].acceptInvitation(gameId, user.id)
        } yield InvitationResult.fromGameInvitation(invitation)

        result.mapErrors
    }

    val rejectInvitation = Endpoints.rejectInvitation.toRoutes {
      case (gameId, token) =>
        val result = for {
          user <- Authentication[F].verify(token)
          invitation <- GameInvitationService[F].rejectInvitation(gameId, user.id)
        } yield InvitationResult.fromGameInvitation(invitation)

        result.mapErrors
    }

    getUsers <+> getUser <+> invitationByEmail <+> userInvitation <+> acceptInvitation <+> rejectInvitation
  }

}
