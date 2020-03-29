package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.infrastructure.uuid.UUIDGenerator
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState

object FixedUUIDGenerator {

  def fixed: UUIDGenerator[TestAppState] = () => StateT { data: TestAppData =>
    IO.pure((data.copy(uuids = data.uuids.tail), data.uuids.head))
  }

}
