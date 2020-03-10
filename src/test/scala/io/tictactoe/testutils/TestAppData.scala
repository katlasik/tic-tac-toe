package io.tictactoe.testutils

import java.time.Instant
import java.util.UUID

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.authentication.model.{ConfirmationToken, User}
import io.tictactoe.emails.EmailMessage
import io.tictactoe.events.bus.Event

final case class TestAppData(
    uuids: List[UUID] = Nil,
    infoMessages: List[String] = Nil,
    errorMessages: List[String] = Nil,
    users: List[User] = Nil,
    dates: List[Instant] = Nil,
    events: List[Event] = Nil,
    confirmationTokens: List[ConfirmationToken] = Nil,
    emails: List[EmailMessage] = Nil
)

object TestAppData {
  type TestAppState[V] = StateT[IO, TestAppData, V]
}
