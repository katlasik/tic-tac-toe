package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.base.logging.{Logger, Logging}
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState

import scala.reflect.ClassTag

object MemoryLogging {

  def memory: Logging[TestAppState] = new Logging[TestAppState] {
    override def create[C: ClassTag]: TestAppState[Logger[TestAppState]] = {

      val logger = new Logger[TestAppState] {
        override def info(msg: => String): TestAppState[Unit] = StateT { data: TestAppData =>
          IO.pure((data.copy(infoMessages = msg :: data.infoMessages), ()))
        }

        override def error(msg: => String, throwable: Throwable): TestAppState[Unit] = StateT { data: TestAppData =>
          IO.pure((data.copy(errorMessages = s"$msg: ${throwable.getMessage}"  :: data.errorMessages), ()))
        }
      }

      StateT.pure(logger);

    }
  }

}
