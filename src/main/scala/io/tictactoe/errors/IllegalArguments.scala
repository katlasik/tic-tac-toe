package io.tictactoe.errors

final case class IllegalArguments(override val msg: String) extends BaseError(msg)
