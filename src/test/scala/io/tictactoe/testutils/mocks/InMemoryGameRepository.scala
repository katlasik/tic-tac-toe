package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.modules.game.infrastructure.repositories.GameRepository
import io.tictactoe.modules.game.model.Game
import io.tictactoe.testutils.TestAppData.TestAppState
object InMemoryGameRepository {

  def inMemory: GameRepository[TestAppState] = new GameRepository[TestAppState] {
    override def save(game: Game): TestAppState[Game] = StateT{ data =>
      IO.pure((data.copy(games = game :: data.games), game))
    }
  }

}
