package io.tictactoe

import cats.effect.{ContextShift, ExitCode, IO, IOApp, Timer}
import io.tictactoe.server.{ApplicationExecutionContexts, Server}

object EntryPoint extends IOApp {

  implicit val cs: ContextShift[IO] = IO.contextShift(ApplicationExecutionContexts.main)
  implicit val t: Timer[IO] = IO.timer(ApplicationExecutionContexts.main)

  override def run(args: List[String]): IO[ExitCode] = Server.stream[IO].compile.drain.as(ExitCode.Success)
}
