package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import cats.implicits._
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.modules.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.modules.users.infrastructure.repositories.UserRepository
import io.tictactoe.values.{Confirmed, Email, UserId}
import henkan.convert.Syntax._

object InMemoryUserRepository {

  def inMemory: UserRepository[TestAppState] = new UserRepository[TestAppState] {

    override def confirmedUsers(): TestAppState[List[SimpleUser]] = StateT { data: TestAppData =>
      IO.pure((data, data.users.filter(_.isConfirmed === Confirmed).map(_.to[SimpleUser]())))
    }

    override def getById(id: UserId): TestAppState[Option[DetailedUser]] =  StateT { data: TestAppData =>
      IO.pure((data, data.users.find(_.id === id).map(_.to[DetailedUser]())))
    }

    override def getByEmail(email: Email): TestAppState[Option[DetailedUser]] =  StateT { data: TestAppData =>
      IO.pure((data, data.users.find(_.email === email).map(_.to[DetailedUser]())))
    }

  }

}
