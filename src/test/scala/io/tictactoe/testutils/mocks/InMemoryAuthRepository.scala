package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.implicits._
import cats.effect.IO
import io.tictactoe.authentication.model.User
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.values.{Email, UserId, Username, Yes}

object InMemoryAuthRepository {

  def inMemory: AuthRepository[TestAppState] = new AuthRepository[TestAppState] {
    override def getByEmail(email: Email): TestAppState[Option[User]] = StateT { data: TestAppData =>
      IO.pure((data, data.users.find(_.email === email)))
    }

    override def existsByEmail(email: Email): TestAppState[Boolean] = StateT { data: TestAppData =>
      IO.pure((data, data.users.exists(_.email === email)))
    }

    override def existsByName(username: Username): TestAppState[Boolean] = StateT { data: TestAppData =>
      IO.pure((data, data.users.exists(_.username === username)))
    }

    override def save(user: User): TestAppState[User] = StateT { data: TestAppData =>
      IO.pure((data.copy(users = user :: data.users), user))
    }

    override def getById(id: UserId): TestAppState[Option[User]] = StateT { data: TestAppData =>
      IO.pure((data, data.users.find(_.id === id)))
    }

    override def confirm(user: User): TestAppState[User] = StateT { data: TestAppData =>
      val confirmed = user.copy(isConfirmed = Yes, confirmationToken = None)
      IO.pure((data.copy(users = confirmed :: data.users.filter(_.id =!= confirmed.id)), confirmed))
    }
  }

}
