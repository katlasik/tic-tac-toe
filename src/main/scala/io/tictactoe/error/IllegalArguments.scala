package io.tictactoe.error

final case class IllegalArguments(override val msg: String) extends BaseError(msg)
