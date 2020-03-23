package io.tictactoe.testutils

import java.time.Instant
import java.util.UUID

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.authentication.model.User
import io.tictactoe.base.tokens.values.ConfirmationToken
import io.tictactoe.emails.model.{EmailMessage, MissingEmail}
import io.tictactoe.events.bus.Event

final case class TestAppData(
    uuids: List[UUID] = Nil,
    infoMessages: List[String] = Nil,
    errorMessages: List[String] = Nil,
    users: List[User] = Nil,
    dates: List[Instant] = Nil,
    events: List[Event] = Nil,
    tokens: List[ConfirmationToken] = Nil,
    sentEmails: List[EmailMessage] = Nil,
    savedEmails: List[EmailMessage] = Nil,
    missingEmails: List[MissingEmail] = Nil
)

object TestAppData {
  type TestAppState[V] = StateT[IO, TestAppData, V]
}
