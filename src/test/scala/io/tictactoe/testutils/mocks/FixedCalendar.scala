package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.calendar.Calendar
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState

object FixedCalendar {

  def fixed: Calendar[TestAppState] = () => StateT { data: TestAppData =>
    IO.pure((data.copy(dates = data.dates.tail), data.dates.head))
  }

}
