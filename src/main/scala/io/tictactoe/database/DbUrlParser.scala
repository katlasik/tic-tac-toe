package io.tictactoe.database

import cats.implicits._
import io.tictactoe.database.errors.{DbUrlParserError, IllegalPortError, UnsupportedDatabaseUrlError}

object DbUrlParser {

  def parse(url: String): Either[DbUrlParserError, DatabaseConfig] = {
    url match {
      case s"postgres://$user:$password@$host:$port/$name" =>
        port.toIntOption match {
          case Some(p) =>
            DatabaseConfig(
              user,
              password,
              s"jdbc:postgresql://$host:$port/$name",
              name,
              p,
              host
            ).asRight
          case None => IllegalPortError(url).asLeft
        }
      case _ => UnsupportedDatabaseUrlError(url).asLeft
    }
  }

}
