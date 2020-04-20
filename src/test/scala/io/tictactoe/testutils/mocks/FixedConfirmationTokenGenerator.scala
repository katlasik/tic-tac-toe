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
      data.tokens match {
        case head :: tail => IO.pure((data.copy(tokens = tail), head))
        case _            => IO.raiseError(new IllegalArgumentException("The confirmation tokens' list is empty."))
      }
    }
  }
}
