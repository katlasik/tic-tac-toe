package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.values.{Hash, Password}
import cats.implicits._
import io.tictactoe.modules.authentication.infrastructure.effects.PasswordHasher

object BypassingPasswordHasher {

  def bypassing: PasswordHasher[TestAppState] = new PasswordHasher[TestAppState] {
    override def hash(password: Password): TestAppState[Hash] = StateT.pure(Hash(password.value))

    override def check(password: Password, hash: Hash): TestAppState[Boolean] = StateT.liftF(IO.pure(password.value === hash.value))
  }

}
