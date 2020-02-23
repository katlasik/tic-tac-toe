package io.tictactoe.database.errors

final case class IllegalPortError(url: String) extends DbUrlParserError("Port must be numeric.")
