package io.tictactoe.game.services

import cats.effect.Sync
import io.tictactoe.game.values.GameId
import io.tictactoe.users.services.UserService
import io.tictactoe.values.{Email, UserId}
import cats.implicits._
import io.tictactoe.authentication.errors.ResourceNotFound
import io.tictactoe.error.IllegalApplicationState
import io.tictactoe.game.errors.InviteSelfError
import io.tictactoe.game.infrastructure.emails.InvitationEmail
import io.tictactoe.game.infrastructure.repositories.InvitationRepository
import io.tictactoe.game.model.GameInvitation
import io.tictactoe.infrastructure.logging.Logging
import io.tictactoe.infrastructure.syntax._
import io.tictactoe.infrastructure.tokens.TokenGenerator
import io.tictactoe.infrastructure.uuid.UUIDGenerator
import io.tictactoe.users.model.DetailedUser

trait GameInvitationService[F[_]] {

  def inviteByEmail(inviterId: UserId, inviteeEmail: Email): F[GameInvitation]

  def inviteById(inviterId: UserId, inviteeId: UserId): F[GameInvitation]
}

object GameInvitationService {

  def apply[F[_]](implicit ev: GameInvitationService[F]): GameInvitationService[F] = ev

  def live[F[_]: UserService: Sync: UUIDGenerator: TokenGenerator: Logging: InvitationEmail: InvitationRepository]
      : F[GameInvitationService[F]] =
    for {
      logger <- Logging[F].create[GameInvitationService[F]]
    } yield
      new GameInvitationService[F] {

        def inviteRegistered(inviter: DetailedUser, invitee: DetailedUser): F[GameInvitation] =
          for {
            gameId <- GameId.next
            invitation <- InvitationRepository[F].save(GameInvitation(gameId, inviter.id, invitee.id.some, none, none))
            _ <- InvitationEmail[F].sendInvitationNotification(inviter, invitee, gameId)
          } yield invitation

        def inviteUnregistered(inviter: DetailedUser, inviteeEmail: Email): F[GameInvitation] =
          for {
            token <- TokenGenerator[F].generate
            gameId <- GameId.next
            invitation <- InvitationRepository[F].save(GameInvitation(gameId, inviter.id, none, token.some, inviteeEmail.some))
            _ <- InvitationEmail[F].sendInvitationEmail(inviteeEmail, inviter, gameId, token)
          } yield invitation

        override def inviteByEmail(inviterId: UserId, inviteeEmail: Email): F[GameInvitation] =
          for {
            inviter <- UserService[F]
              .get(inviterId)
              .throwIfEmpty(IllegalApplicationState(show"User with id = $inviterId no longer exists."))
            maybeInvitee <- UserService[F].getByEmail(inviteeEmail)
            _ <- Sync[F].whenA(maybeInvitee.isDefined)(
              logger.info(show"User with email $inviteeEmail was already registered, sending notification email.")
            )
            invitation <- maybeInvitee.fold(inviteUnregistered(inviter, inviteeEmail))(
              user => Sync[F].pure(inviterId).ensure(InviteSelfError)(_ =!= user.id) *> inviteRegistered(inviter, user)
            )
          } yield invitation

        override def inviteById(inviterId: UserId, inviteeId: UserId): F[GameInvitation] =
          for {
            _ <- Sync[F].pure(inviterId).ensure(InviteSelfError)(_ =!= inviteeId)
            inviter <- UserService[F]
              .get(inviterId)
              .throwIfEmpty(IllegalApplicationState(show"User with id = $inviterId no longer exists."))
            invitee <- UserService[F]
              .get(inviteeId)
              .throwIfEmpty(ResourceNotFound)
            invitation <- inviteRegistered(inviter, invitee)
          } yield invitation

      }

}
