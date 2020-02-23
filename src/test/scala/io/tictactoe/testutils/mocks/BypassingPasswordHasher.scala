package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.authentication.services.{Hash, PasswordHasher}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.values.Password
import cats.implicits._

object BypassingPasswordHasher {

  def bypassing: PasswordHasher[TestAppState] = new PasswordHasher[TestAppState] {
    override def hash(password: Password): TestAppState[Hash] = StateT.pure(Hash(password.value))

    override def check(password: Password, hash: Hash): TestAppState[Boolean] = StateT.liftF(IO.pure(password.value === hash.value))
  }

}
