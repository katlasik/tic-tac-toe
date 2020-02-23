package io.tictactoe.configuration

import cats.effect.Sync
import pureconfig.ConfigSource
import cats.implicits._
import pureconfig.generic.auto._

trait Configuration[F[_]] {
  def access(): F[ConfigurationProperties]
}

object Configuration {

  implicit def apply[F[_]]()(implicit ev: Configuration[F]): Configuration[F] = ev

  def load[F[_]: Sync]: F[Configuration[F]] =
    for {
      c <- Sync[F].fromEither(
        ConfigSource.default.load[ConfigurationProperties].leftMap(ConfigurationLoadingError)
      )
    } yield
      new Configuration[F] {
        override def access(): F[ConfigurationProperties] = Sync[F].pure(c)
      }

}
