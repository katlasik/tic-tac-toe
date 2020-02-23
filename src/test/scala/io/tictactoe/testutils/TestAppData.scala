package io.tictactoe.testutils

import java.util.UUID

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.authentication.model.User

final case class TestAppData(
    uuids: List[UUID] = Nil,
    infoMessages: List[String] = Nil,
    errorMessages: List[String] = Nil,
    users: List[User] = Nil
)

object TestAppData {
  type TestAppState[V] = StateT[IO, TestAppData, V]
}
