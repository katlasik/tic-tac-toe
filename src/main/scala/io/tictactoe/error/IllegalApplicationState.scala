package io.tictactoe.error

final case class IllegalApplicationState(override val msg: String) extends BaseError(msg)
