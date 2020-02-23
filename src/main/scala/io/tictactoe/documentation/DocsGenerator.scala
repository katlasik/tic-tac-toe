package io.tictactoe.documentation

import cats.effect.Sync
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import cats.implicits._
import better.files._
import io.tictactoe.base.logging.Logging
import io.tictactoe.configuration.{Configuration, ConfigurationProperties}
import io.tictactoe.routes.Endpoints

trait DocsGenerator[F[_]] {
  def generate(): F[Unit]
}

object DocsGenerator {

  def apply[F[_]](implicit ev: DocsGenerator[F]): DocsGenerator[F] = ev

  def init[F[_]: Configuration: Sync: Logging]: DocsGenerator[F] = new DocsGenerator[F] {

    def write(yaml: String, configuration: ConfigurationProperties): F[Unit] = {

      val target = configuration.docs.file

      for {
        logger <- Logging[F].create[DocsGenerator[F]]
        _ <- logger.info(show"Generating and saving documentation to file $target.")
        _ <- Sync[F].delay(target.toFile.overwrite(yaml))
      } yield ()
    }

    override def generate(): F[Unit] =
      for {
        config <- Configuration[F].access()
        yaml = Endpoints.all.toOpenAPI(config.app.name, config.app.version).toYaml
        _ <- write(yaml, config).attempt
      } yield ()

  }

}
