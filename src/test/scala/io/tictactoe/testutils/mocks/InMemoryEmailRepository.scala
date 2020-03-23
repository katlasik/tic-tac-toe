package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import cats.implicits._
import io.tictactoe.emails.model.{EmailMessage, MissingEmail}
import io.tictactoe.emails.services.EmailRepository
import io.tictactoe.emails.values.MailId
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState
import henkan.convert.Syntax._

object InMemoryEmailRepository {

  def inMemory: EmailRepository[TestAppState] = new EmailRepository[TestAppState] {
    override def save(email: EmailMessage): TestAppState[MailId] = StateT { data: TestAppData =>
      val mailId = MailId(data.uuids.head)
      val missingEmail = email.to[MissingEmail].set(id = mailId)

      IO.pure(
        (
          data.copy(uuids = data.uuids.tail, savedEmails = email :: data.savedEmails, missingEmails = missingEmail :: data.missingEmails),
          mailId
        )
      )
    }

    override def confirm(mailId: MailId): TestAppState[Unit] = StateT { data: TestAppData =>
      IO.pure((data.copy(missingEmails = data.missingEmails.filter(_.id =!= mailId)), ()))
    }

    override def missingEmails(): TestAppState[List[MissingEmail]] = StateT { data: TestAppData =>
      IO.pure((data, data.missingEmails))
    }
  }

}
