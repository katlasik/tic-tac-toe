package io.tictactoe.testutils

import java.time.Instant
import java.util.UUID

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.authentication.model.User
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.emails.model.MissingEmail
import io.tictactoe.game.model.GameInvitation
import io.tictactoe.infrastructure.emails.model.EmailMessage
import io.tictactoe.infrastructure.events.model.Event

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
    missingEmails: List[MissingEmail] = Nil,
    invitations: List[GameInvitation] = Nil
)

object TestAppData {
  type TestAppState[V] = StateT[IO, TestAppData, V]
}
