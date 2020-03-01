package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.events.bus.{Event, EventBus}
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState

object InMemoryEventBus  {

  def inMemory: EventBus[TestAppState] = new EventBus[TestAppState] {
    override def publish(event: Event): TestAppState[Unit] = StateT { data =>
      IO.pure((data.copy(events = event :: data.events), ()))
    }

    override def publishF(event: TestAppState[Event]): TestAppState[Unit] = for {
      e <- event
      t <- StateT { data : TestAppData => IO.pure((data.copy(events = e :: data.events), ()))}
    } yield t
  }

}
