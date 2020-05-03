package io.tictactoe.utilities.database.errors

import scala.util.control.NoStackTrace

class DbUrlParserError(val msg: String) extends Throwable(msg) with NoStackTrace
