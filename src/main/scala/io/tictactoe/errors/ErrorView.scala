package io.tictactoe.errors

final case class ErrorView(message: String)

object ErrorView {
  def fromThrowable(t: Throwable): ErrorView = new ErrorView(t.getMessage)
}
