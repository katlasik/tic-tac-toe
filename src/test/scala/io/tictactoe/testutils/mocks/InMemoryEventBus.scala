package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.utilities.events.EventBus
import io.tictactoe.utilities.events.model.Event
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState
import fs2.Stream

object InMemoryEventBus  {

  def inMemory: EventBus[TestAppState] = new EventBus[TestAppState] {
    override def publish(event: Event): TestAppState[Unit] = StateT { data =>
      IO.pure((data.copy(events = event :: data.events), ()))
    }

    override def publishF(event: TestAppState[Event]): TestAppState[Unit] = for {
      e <- event
      t <- StateT { data : TestAppData => IO.pure((data.copy(events = e :: data.events), ()))}
    } yield t

    override def subscribe: Stream[TestAppState, Event] =  Stream.evalSeq(StateT { data =>
      IO.pure((data, data.events))
    })
  }

}
