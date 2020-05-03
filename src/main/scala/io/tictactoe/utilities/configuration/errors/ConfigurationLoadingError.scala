package io.tictactoe.utilities.configuration.errors

import io.tictactoe.errors.BaseError
import pureconfig.error.ConfigReaderFailures
import cats.implicits._

final case class ConfigurationLoadingError(failures: ConfigReaderFailures)
    extends BaseError(
      show"Configuration loading failed with following errors: ${failures.toList.map(_.description).mkString(", ")}"
    )
