package io.tictactoe.errors

final case class IllegalApplicationState(override val msg: String) extends BaseError(msg)
