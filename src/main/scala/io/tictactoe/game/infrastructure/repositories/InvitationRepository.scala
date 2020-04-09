package io.tictactoe.game.infrastructure.repositories

import cats.effect.Sync
import doobie.hikari.HikariTransactor
import io.tictactoe.database.Database
import io.tictactoe.game.model.GameInvitation
import doobie.implicits._
import doobie.postgres.implicits._
import cats.implicits._

trait InvitationRepository[F[_]] {

  def save(invitation: GameInvitation): F[GameInvitation]

}

object InvitationRepository {

  def apply[F[_]](implicit ev: InvitationRepository[F]): InvitationRepository[F] = ev

  def postgresql[F[_]: Sync: Database]: InvitationRepository[F] = new InvitationRepository[F] {
    val transactor: HikariTransactor[F] = Database[F].transactor()

    override def save(invitation: GameInvitation): F[GameInvitation] = invitation match {
      case GameInvitation(id, ownerId, guestId, token, guestEmail) =>
        for {
          _ <- sql"INSERT INTO game_invitations(id, owner_id, guest_id, token, guest_email) VALUES ($id, $ownerId, $guestId, $token, $guestEmail)".update.run
            .transact(transactor)
        } yield invitation
    }
  }

}
