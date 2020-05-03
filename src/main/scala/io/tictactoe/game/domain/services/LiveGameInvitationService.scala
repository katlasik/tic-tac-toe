package io.tictactoe.game.domain.services

import cats.effect.Sync
import io.tictactoe.values.{Email, GameId, UserId}
import cats.implicits._
import io.tictactoe.utilities.authorization.Authorized.canReadAndWrite
import io.tictactoe.utilities.authorization.errors.AccessForbidden
import io.tictactoe.errors.{IllegalApplicationState, ResourceNotFound}
import io.tictactoe.game.errors.InviteSelfError
import io.tictactoe.game.infrastructure.services.GameInvitationService
import io.tictactoe.game.infrastructure.emails.InvitationEmail
import io.tictactoe.game.infrastructure.repositories.InvitationRepository
import io.tictactoe.game.model.{GameInvitation, PendingGameInvitation}
import io.tictactoe.game.model.GameInvitationStatus.Pending
import io.tictactoe.users.infrastructure.services.UserService
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.syntax._
import io.tictactoe.utilities.tokens.TokenGenerator
import io.tictactoe.utilities.uuid.UUIDGenerator
import io.tictactoe.users.model.DetailedUser
import mouse.boolean._

object LiveGameInvitationService {

  def live[F[_]: Sync: UUIDGenerator: TokenGenerator: Logging](
      invitationRepository: InvitationRepository[F],
      invitationEmail: InvitationEmail[F],
      userService: UserService[F]
  ): F[GameInvitationService[F]] =
    for {
      logger <- Logging[F].create[GameInvitationService[F]]
    } yield
      new GameInvitationService[F] {

        def inviteRegistered(inviter: DetailedUser, invitee: DetailedUser): F[GameInvitation] =
          for {
            gameId <- GameId.next[F]
            invitation <- invitationRepository.save(GameInvitation.withGuestId(gameId, inviter.id, invitee.id))
            _ <- invitationEmail.sendInvitationNotification(inviter, invitee, gameId)
          } yield invitation

        def inviteUnregistered(inviter: DetailedUser, inviteeEmail: Email): F[GameInvitation] =
          for {
            token <- TokenGenerator[F].generate
            gameId <- GameId.next[F]
            invitation <- invitationRepository.save(GameInvitation.withEmail(gameId, inviter.id, inviteeEmail, token))
            _ <- invitationEmail.sendInvitationEmail(inviteeEmail, inviter, gameId, token)
          } yield invitation

        override def inviteByEmail(inviterId: UserId, inviteeEmail: Email): F[GameInvitation] =
          for {
            inviter <- userService
              .get(inviterId)
              .throwIfEmpty(IllegalApplicationState(show"User with id = $inviterId no longer exists."))
            maybeInvitee <- userService.getByEmail(inviteeEmail)
            _ <- logger
              .info(show"User with email $inviteeEmail was already registered, sending notification email.")
              .whenA(maybeInvitee.isDefined)
            invitation <- maybeInvitee.fold(inviteUnregistered(inviter, inviteeEmail))(
              user => Sync[F].pure(inviterId).ensure(InviteSelfError)(_ =!= user.id) *> inviteRegistered(inviter, user)
            )
          } yield invitation

        override def inviteById(inviterId: UserId, inviteeId: UserId): F[GameInvitation] =
          for {
            _ <- Sync[F].pure(inviterId).ensure(InviteSelfError)(_ =!= inviteeId)
            inviter <- userService
              .get(inviterId)
              .throwIfEmpty(IllegalApplicationState(show"User with id = $inviterId no longer exists."))
            invitee <- userService
              .get(inviteeId)
              .throwIfEmpty(ResourceNotFound)
            invitation <- inviteRegistered(inviter, invitee)
          } yield invitation

        def getPending(gameId: GameId): F[PendingGameInvitation] =
          invitationRepository
            .get(gameId)
            .flatMap {
              case Some(p: PendingGameInvitation) => Sync[F].pure(p.some)
              case _                              => Sync[F].pure(none[PendingGameInvitation])
            }
            .throwIfEmpty(ResourceNotFound)

        override def acceptInvitation(gameId: GameId, inviteeId: UserId): F[GameInvitation] =
          for {
            invitation <- getPending(gameId)
            _ <- canReadAndWrite(inviteeId, invitation).ensure(AccessForbidden)(_.guestId.contains(inviteeId))
            saved <- invitationRepository.accept(gameId)
          } yield saved

        override def acceptInvitationAndSetInvitee(gameId: GameId, inviteeId: UserId): F[GameInvitation] =
          invitationRepository.acceptAndSetGuest(gameId, inviteeId).widen

        override def rejectInvitation(gameId: GameId, participantId: UserId): F[GameInvitation] =
          for {
            invitation <- invitationRepository.get(gameId).throwIfEmptyFilter(ResourceNotFound)(_.status === Pending)
            _ <- canReadAndWrite(participantId, invitation)
            saved <- (participantId === invitation.ownerId).fold(
              invitationRepository.cancel(gameId),
              invitationRepository.reject(gameId)
            )
          } yield saved

        override def get(gameId: GameId): F[Option[GameInvitation]] = invitationRepository.get(gameId)

      }

}