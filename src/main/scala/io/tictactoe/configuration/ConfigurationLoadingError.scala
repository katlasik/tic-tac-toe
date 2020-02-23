package io.tictactoe.configuration

import io.tictactoe.error.BaseError
import pureconfig.error.ConfigReaderFailures
import cats.implicits._

final case class ConfigurationLoadingError(failures: ConfigReaderFailures)
    extends BaseError(
      show"Configuration loading failed with following errors: ${failures.toList.map(_.description).mkString(", ")}"
    )
