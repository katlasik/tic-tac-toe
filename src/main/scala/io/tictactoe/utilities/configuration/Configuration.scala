package io.tictactoe.utilities.configuration

import java.nio.file.Path

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.utilities.configuration.errors.ConfigurationLoadingError
import io.tictactoe.utilities.configuration.model.ConfigurationProperties
import pureconfig.ConfigSource
import pureconfig.generic.auto._

trait Configuration[F[_]] {
  def access(): F[ConfigurationProperties]
}

object Configuration {

  def apply[F[_]]()(implicit ev: Configuration[F]): Configuration[F] = ev

  def loadFromFile[F[_]: Sync](path: Path): F[Configuration[F]] = process(ConfigSource.file(path))

  def load[F[_]: Sync]: F[Configuration[F]] = process(ConfigSource.default)

  private def process[F[_]: Sync](source: ConfigSource): F[Configuration[F]] =
    for {
      c <- Sync[F].fromEither(
        source.load[ConfigurationProperties].leftMap(ConfigurationLoadingError)
      )
    } yield
      new Configuration[F] {
        override def access(): F[ConfigurationProperties] = Sync[F].pure(c)
      }

}
