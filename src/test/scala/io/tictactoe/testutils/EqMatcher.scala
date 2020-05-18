package io.tictactoe.testutils

import org.scalactic.Equality
import org.scalatest.Matchers

trait EqMatcher { _: Matchers =>

  implicit class ShouldEqOps[A: Equality](left: A) {
    def shouldEq(right: A) = left shouldEqual right
  }

}
