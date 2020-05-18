package io.tictactoe.modules.game.domain.services

import cats.effect.Sync
import io.tictactoe.modules.game.infrastructure.repositories.GameRepository
import io.tictactoe.modules.game.infrastructure.services.GameService
import io.tictactoe.modules.game.model.Game
import io.tictactoe.utilities.random.RandomPicker
import io.tictactoe.values.{GameId, UserId}
import cats.implicits._

object LiveGameService {

  def live[F[_]: Sync: RandomPicker](gameRepository: GameRepository[F]): GameService[F] = new GameService[F] {
    override def createGame(gameId: GameId, hostId: UserId, guestIt: UserId): F[Unit] = for {
      initialPlayerId <- RandomPicker[F].pickOne(hostId, guestIt)
      _ <- gameRepository.save(Game(gameId, hostId, guestIt, initialPlayerId))
    } yield ()
  }


}
