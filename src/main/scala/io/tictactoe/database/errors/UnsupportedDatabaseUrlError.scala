package io.tictactoe.database.errors

import cats.implicits._

final case class UnsupportedDatabaseUrlError(url: String) extends DbUrlParserError(show"Illegal database URL: $url.")
