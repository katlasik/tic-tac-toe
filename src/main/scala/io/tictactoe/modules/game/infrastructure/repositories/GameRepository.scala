package io.tictactoe.modules.game.infrastructure.repositories

import cats.effect.Sync
import doobie.hikari.HikariTransactor
import io.tictactoe.utilities.database.Database
import io.tictactoe.modules.game.model.Game
import doobie.implicits._
import doobie.postgres.implicits._
import cats.implicits._
import cats.implicits._

trait GameRepository[F[_]] {
  def save(game: Game): F[Game]
}

object GameRepository {

  def live[F[_]: Sync: Database]: GameRepository[F] = new GameRepository[F] {
    val transactor: HikariTransactor[F] = Database[F].transactor()

    override def save(game: Game): F[Game] = game match {
      case Game(gameId, hostId, guestId, initialPlayer) =>
        sql"INSERT INTO games(id, owner_id, guest_id, initial_player_id) VALUES ($gameId, $hostId, $guestId, $initialPlayer)".update.run.transact(transactor) >> game.pure[F]
    }
  }

}


