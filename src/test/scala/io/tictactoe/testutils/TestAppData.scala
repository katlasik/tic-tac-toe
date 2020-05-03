package io.tictactoe.testutils

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import cats.Monoid
import cats.data.StateT
import cats.effect.IO
import io.tictactoe.modules.authentication.model.User
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.modules.game.model.GameInvitation
import io.tictactoe.utilities.emails.model.{EmailMessage, MissingEmail}
import io.tictactoe.utilities.events.model.Event
import cats.derived.semi.monoid
import cats.implicits._

final case class TestAppData(
    uuids: List[UUID] = Nil,
    infoMessages: List[String] = Nil,
    errorMessages: List[String] = Nil,
    users: List[User] = Nil,
    instants: List[Instant] = Nil,
    dates: List[LocalDateTime] = Nil,
    events: List[Event] = Nil,
    tokens: List[ConfirmationToken] = Nil,
    sentEmails: List[EmailMessage] = Nil,
    savedEmails: List[EmailMessage] = Nil,
    missingEmails: List[MissingEmail] = Nil,
    invitations: List[GameInvitation] = Nil
)

object TestAppData {

  type TestAppState[V] = StateT[IO, TestAppData, V]

  implicit val m: Monoid[TestAppData] = monoid[TestAppData]

}
