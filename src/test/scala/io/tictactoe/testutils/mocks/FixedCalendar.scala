package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.utilities.calendar.Calendar
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState

object FixedCalendar {

  def fixed: Calendar[TestAppState] = () => StateT { data: TestAppData =>
    data.instants match {
      case head :: instantsTail => IO.pure((data.copy(instants = instantsTail), head))
      case _ =>IO.raiseError(new IllegalArgumentException("The instants list is empty!"))
    }

  }

}
