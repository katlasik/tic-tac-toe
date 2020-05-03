package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.implicits._
import cats.effect.IO
import io.tictactoe.authentication.infrastructure.effects.Hash
import io.tictactoe.authentication.model.User
import io.tictactoe.authentication.infrastructure.repositories.AuthRepository
import io.tictactoe.errors.ResourceNotFound
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.values.{Confirmed, Email, UserId, Username}

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
      val confirmed = user.copy(isConfirmed = Confirmed, registrationConfirmationToken = None)
      IO.pure((data.copy(users = confirmed :: data.users.filter(_.id =!= confirmed.id)), confirmed))
    }

    override def updateRegistrationConfirmationToken(userId: UserId, token: ConfirmationToken): TestAppState[Unit] = StateT { data: TestAppData =>
      data.users.find(user => user.id === userId) match {
        case Some(user) =>
          IO.pure((data.copy(users = user.copy(registrationConfirmationToken = token.some) :: data.users.filter(_.id =!= user.id)), ()))
        case None => IO.raiseError(ResourceNotFound)
      }
    }

    override def updatePasswordResetToken(userId: UserId, token: ConfirmationToken): TestAppState[Unit] = StateT { data: TestAppData =>
      data.users.find(user => user.id === userId) match {
        case Some(user) =>
          IO.pure((data.copy(users = user.copy(passwordResetToken = token.some) :: data.users.filter(_.id =!= user.id)), ()))
        case None => IO.raiseError(ResourceNotFound)
      }
    }

    override def updateHash(userId: UserId, hash: Hash): TestAppState[Unit] = StateT { data: TestAppData =>
      data.users.find(user => user.id === userId) match {
        case Some(user) =>
          IO.pure((data.copy(users = user.copy(hash = hash) :: data.users.filter(_.id =!= user.id)), ()))
        case None => IO.raiseError(ResourceNotFound)
      }
    }
  }

}
