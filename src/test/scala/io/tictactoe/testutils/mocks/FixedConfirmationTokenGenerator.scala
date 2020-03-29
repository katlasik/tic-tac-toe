package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.infrastructure.tokens.TokenGenerator
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState

object FixedConfirmationTokenGenerator {

  def fixed: TokenGenerator[TestAppState] = new TokenGenerator[TestAppState] {
    override def generate: TestAppState[ConfirmationToken] = StateT { data: TestAppData =>
      IO.pure((data.copy(tokens = data.tokens.tail), data.tokens.head))
    }
  }
}
