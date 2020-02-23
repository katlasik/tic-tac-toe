package io.tictactoe

import cats.effect.{ExitCode, IO, IOApp}
import io.tictactoe.server.Server
import cats.implicits._

object EntryPoint extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = Server.stream[IO].compile.drain.as(ExitCode.Success)
}
