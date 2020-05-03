package io.tictactoe.errors

import scala.util.control.NoStackTrace

class BaseError(val msg: String) extends Throwable(msg) with NoStackTrace
