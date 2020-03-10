package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.emails.EmailMessage
import io.tictactoe.emails.services.EmailSender
import io.tictactoe.testutils.TestAppData.TestAppState

object InMemoryEmailSender {

  def inMemory: EmailSender[TestAppState] = new EmailSender[TestAppState] {
    override def send(email: EmailMessage): TestAppState[Unit] = StateT{ data =>
      IO.pure((data.copy(emails = email :: data.emails), ()))
    }
  }

}
