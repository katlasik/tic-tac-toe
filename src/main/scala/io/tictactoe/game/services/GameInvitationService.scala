package io.tictactoe.game.services

import cats.effect.Sync
import io.tictactoe.game.values.GameId
import io.tictactoe.users.services.UserService
import io.tictactoe.values.{Email, UserId}
import cats.implicits._
import io.tictactoe.authentication.errors.ResourceNotFound
import io.tictactoe.authorization.Authorized.canReadAndWrite
import io.tictactoe.authorization.errors.AccessForbidden
import io.tictactoe.error.IllegalApplicationState
import io.tictactoe.game.errors.InviteSelfError
import io.tictactoe.game.infrastructure.emails.InvitationEmail
import io.tictactoe.game.infrastructure.repositories.InvitationRepository
import io.tictactoe.game.model.{GameInvitation, PendingGameInvitation}
import io.tictactoe.game.model.GameInvitationStatus.Pending
import io.tictactoe.infrastructure.logging.Logging
import io.tictactoe.infrastructure.syntax._
import io.tictactoe.infrastructure.tokens.TokenGenerator
import io.tictactoe.infrastructure.uuid.UUIDGenerator
import io.tictactoe.users.model.DetailedUser
import mouse.boolean._

trait GameInvitationService[F[_]] {
  def acceptInvitation(gameId: GameId, inviteeId: UserId): F[GameInvitation]

  def inviteByEmail(inviterId: UserId, inviteeEmail: Email): F[GameInvitation]

  def inviteById(inviterId: UserId, inviteeId: UserId): F[GameInvitation]

  def rejectInvitation(gameId: GameId, inviteeId: UserId): F[GameInvitation]

  def get(gameId: GameId): F[Option[GameInvitation]]

  def acceptInvitationAndSetInvitee(gameId: GameId, inviteeId: UserId): F[GameInvitation]
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
            gameId <- GameId.next[F]
            invitation <- InvitationRepository[F].save(GameInvitation.withGuestId(gameId, inviter.id, invitee.id))
            _ <- InvitationEmail[F].sendInvitationNotification(inviter, invitee, gameId)
          } yield invitation

        def inviteUnregistered(inviter: DetailedUser, inviteeEmail: Email): F[GameInvitation] =
          for {
            token <- TokenGenerator[F].generate
            gameId <- GameId.next[F]
            invitation <- InvitationRepository[F].save(GameInvitation.withEmail(gameId, inviter.id, inviteeEmail, token))
            _ <- InvitationEmail[F].sendInvitationEmail(inviteeEmail, inviter, gameId, token)
          } yield invitation

        override def inviteByEmail(inviterId: UserId, inviteeEmail: Email): F[GameInvitation] =
          for {
            inviter <- UserService[F]
              .get(inviterId)
              .throwIfEmpty(IllegalApplicationState(show"User with id = $inviterId no longer exists."))
            maybeInvitee <- UserService[F].getByEmail(inviteeEmail)
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
            inviter <- UserService[F]
              .get(inviterId)
              .throwIfEmpty(IllegalApplicationState(show"User with id = $inviterId no longer exists."))
            invitee <- UserService[F]
              .get(inviteeId)
              .throwIfEmpty(ResourceNotFound)
            invitation <- inviteRegistered(inviter, invitee)
          } yield invitation

        def getPending(gameId: GameId): F[PendingGameInvitation] =
          InvitationRepository[F]
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
            saved <- InvitationRepository[F].accept(gameId)
          } yield saved

        override def acceptInvitationAndSetInvitee(gameId: GameId, inviteeId: UserId): F[GameInvitation] =
          InvitationRepository[F].acceptAndSetGuest(gameId, inviteeId).widen

        override def rejectInvitation(gameId: GameId, participantId: UserId): F[GameInvitation] =
          for {
            invitation <- InvitationRepository[F].get(gameId).throwIfEmptyFilter(ResourceNotFound)(_.status === Pending)
            _ <- canReadAndWrite(participantId, invitation)
            saved <- (participantId === invitation.ownerId).fold(
              InvitationRepository[F].cancel(gameId),
              InvitationRepository[F].reject(gameId)
            )
          } yield saved

        override def get(gameId: GameId): F[Option[GameInvitation]] = InvitationRepository[F].get(gameId)

      }

}
