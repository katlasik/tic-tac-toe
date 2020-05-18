package io.tictactoe.server
import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.chaining._

object ApplicationExecutionContexts {

  private val Prefix = "tictactoe-app-"

  val main: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(
        Runtime.getRuntime.availableProcessors(),
        (task: Runnable) =>
          Executors.defaultThreadFactory
            .newThread(task)
            .tap(t => t.setName(Prefix + t.getName))
      )
    )

}
