package io.tictactoe.game.infrastructure.repositories

import java.time.LocalDateTime

import cats.effect.Sync
import doobie.hikari.HikariTransactor
import io.tictactoe.utilities.database.Database
import io.tictactoe.game.model.{AcceptedGameInvitation, CancelledGameInvitation, GameInvitation, PendingGameInvitation, RejectedGameInvitation}
import doobie.implicits._
import doobie.postgres.implicits._
import cats.implicits._
import doobie.implicits.javatime._
import cats.implicits._
import doobie.util.Read
import doobie.util.update.Update
import doobie.util.query.Query
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.values.{Email, GameId, UserId}
import shapeless.HNil

trait InvitationRepository[F[_]] {
  def cancel(gameId: GameId): F[CancelledGameInvitation]

  def reject(gameId: GameId): F[RejectedGameInvitation]

  def accept(gameId: GameId): F[AcceptedGameInvitation]

  def acceptAndSetGuest(gameId: GameId, guestId: UserId): F[AcceptedGameInvitation]

  def save(invitation: PendingGameInvitation): F[GameInvitation]

  def get(gameId: GameId): F[Option[GameInvitation]]

}

object InvitationRepository {

  def apply[F[_]: Sync](implicit ev: InvitationRepository[F]): InvitationRepository[F] = ev

  def postgresql[F[_]: Sync: Database]: InvitationRepository[F] = new InvitationRepository[F] {
    val transactor: HikariTransactor[F] = Database[F].transactor()

    override def save(invitation: PendingGameInvitation): F[GameInvitation] = invitation match {
      case PendingGameInvitation(id, ownerId, guestId, token, guestEmail) =>
        for {
          _ <- sql"""INSERT INTO game_invitations(id, owner_id, guest_id, token, guest_email)
                       VALUES ($id, $ownerId, $guestId, $token, $guestEmail)""".update.run
            .transact(transactor)
        } yield invitation
    }

    override def get(gameId: GameId): F[Option[GameInvitation]] =
      sql"SELECT id, owner_id, guest_id, token, guest_email, accepted_on, rejected_on, cancelled_on FROM game_invitations WHERE id = $gameId"
        .query[
          (
              GameId,
              UserId,
              Option[UserId],
              Option[ConfirmationToken],
              Option[Email],
              Option[LocalDateTime],
              Option[LocalDateTime],
              Option[LocalDateTime]
          )
        ]
        .map {
          case (id, ownerId, Some(guestId), token, guestEmail, Some(acceptedOn), _, _) =>
            AcceptedGameInvitation(id, ownerId, guestId, token, guestEmail, acceptedOn)
          case (id, ownerId, guestId, token, guestEmail, _, Some(rejectedOn), _) =>
            RejectedGameInvitation(id, ownerId, guestId, token, guestEmail, rejectedOn)
          case (id, ownerId, guestId, token, guestEmail, _, _, Some(cancelledOn)) =>
            CancelledGameInvitation(id, ownerId, guestId, token, guestEmail, cancelledOn)
          case (id, ownerId, guestId, token, guestEmail, _, _, _) => PendingGameInvitation(id, ownerId, guestId, token, guestEmail)
        }
        .option
        .transact(transactor)

    def updateAndGet[R <: GameInvitation: Read](gameId: GameId, column: String): F[R] = {
      val query = for {
        _ <- Update[HNil](show"UPDATE game_invitations SET $column = NOW() WHERE id = '$gameId'").toUpdate0(HNil).run
        invitation <- Query[HNil, R](
          show"SELECT id, owner_id, guest_id, token, guest_email, $column FROM game_invitations WHERE id = '$gameId'")
          .toQuery0(HNil)
          .unique
      } yield invitation

      query.transact(transactor)
    }

    override def accept(gameId: GameId): F[AcceptedGameInvitation] = updateAndGet[AcceptedGameInvitation](gameId, "accepted_on")

    override def acceptAndSetGuest(gameId: GameId, guestId: UserId): F[AcceptedGameInvitation] = {
      val query = for {
        _ <- sql"UPDATE game_invitations SET accepted_on = NOW(), guest_id = $guestId WHERE id = $gameId".update.run
        invitation <- sql"SELECT id, owner_id, guest_id, token, guest_email, accepted_on FROM game_invitations WHERE id = $gameId"
          .query[AcceptedGameInvitation]
          .unique
      } yield invitation

      query.transact(transactor)
    }

    override def reject(gameId: GameId): F[RejectedGameInvitation] = updateAndGet[RejectedGameInvitation](gameId, "rejected_on")

    override def cancel(gameId: GameId): F[CancelledGameInvitation] = updateAndGet[CancelledGameInvitation](gameId, "cancelled_on")
  }

}
