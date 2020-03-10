package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.authentication.model.ConfirmationToken
import io.tictactoe.authentication.services.ConfirmationTokenGenerator
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState

object FixedConfirmationTokenGenerator {

  def fixed: ConfirmationTokenGenerator[TestAppState] = new ConfirmationTokenGenerator[TestAppState] {
    override def generate: TestAppState[ConfirmationToken] = StateT { data: TestAppData =>
      IO.pure((data.copy(confirmationTokens = data.confirmationTokens.tail), data.confirmationTokens.head))
    }
  }
}
