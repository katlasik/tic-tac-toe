package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import cats.implicits._
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.users.repositories.UserRepository
import io.tictactoe.values.UserId
import henkan.convert.Syntax._

object InMemoryUserRepository {

  def inMemory: UserRepository[TestAppState] = new UserRepository[TestAppState] {

    override def all(): TestAppState[List[SimpleUser]] = StateT { data: TestAppData =>
      IO.pure(data, data.users.map(_.to[SimpleUser]()))
    }

    override def getById(id: UserId): TestAppState[Option[DetailedUser]] =  StateT { data: TestAppData =>
      IO.pure(data, data.users.find(_.id === id).map(_.to[DetailedUser]()))
    }
  }

}
