package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import cats.implicits._
import io.tictactoe.emails.model.MissingEmail
import io.tictactoe.emails.services.EmailRepository
import io.tictactoe.emails.values.MailId
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState
import henkan.convert.Syntax._
import io.tictactoe.infrastructure.emails.model.EmailMessage

object InMemoryEmailRepository {

  def inMemory: EmailRepository[TestAppState] = new EmailRepository[TestAppState] {
    override def save(email: EmailMessage): TestAppState[MailId] = StateT { data: TestAppData =>
      data.uuids match {
        case uuid :: uuidsTail =>
          val mailId = MailId(uuid)
          val missingEmail = email.to[MissingEmail].set(id = mailId)
          IO.pure(
            (
              data.copy(uuids = uuidsTail, savedEmails = email :: data.savedEmails, missingEmails = missingEmail :: data.missingEmails),
              mailId
            )
          )
        case _ => IO.raiseError(new IllegalArgumentException("The UUIDs list is empty."))
      }

    }

    override def confirm(mailId: MailId): TestAppState[Unit] = StateT { data: TestAppData =>
      IO.pure((data.copy(missingEmails = data.missingEmails.filter(_.id =!= mailId)), ()))
    }

    override def missingEmails(): TestAppState[List[MissingEmail]] = StateT { data: TestAppData =>
      IO.pure((data, data.missingEmails))
    }
  }

}
