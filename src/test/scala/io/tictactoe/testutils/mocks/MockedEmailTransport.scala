package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.emails.model.EmailMessage
import io.tictactoe.emails.services.EmailTransport
import io.tictactoe.testutils.TestAppData.TestAppState

object MockedEmailTransport {

  def mocked: EmailTransport[TestAppState] = new EmailTransport[TestAppState] {
    override def send(email: EmailMessage): TestAppState[Unit] = StateT{ data =>
      IO.pure((data.copy(sentEmails = email :: data.sentEmails), ()))
    }
  }

}
