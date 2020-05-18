package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.utilities.random.RandomInt

object FixedRandomInt {

  def fixed: RandomInt[TestAppState] = new RandomInt[TestAppState] {
    override def int(max: Int): TestAppState[Int] = StateT{ data =>
      data.randomInts match {
        case head :: tail => IO.pure((data.copy(randomInts = tail), head))
        case _ => IO.raiseError(new IllegalArgumentException("The random ints list is empty!"))
      }
    }
  }

}
