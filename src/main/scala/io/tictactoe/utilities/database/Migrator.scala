package io.tictactoe.utilities.database

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.utilities.configuration.Configuration
import org.flywaydb.core.Flyway
import io.tictactoe.utilities.logging.Logging

trait Migrator[F[_]] {
  def migrate: F[Unit]
}

object Migrator {

  def init[F[_]: Sync: Configuration: Logging]: F[Migrator[F]] =
    for {
      logger <- Logging[F].create[Migrator[F]]
      url <- Configuration[F].access().map(_.db.databaseUrl)
      db <- Sync[F].fromEither(DbUrlParser.parse(url))
      flyway <- Sync[F].delay {
        Flyway
          .configure()
          .dataSource(db.jdbcUrl, db.username, db.password)
          .load()
      }
    } yield
      new Migrator[F] {
        override def migrate: F[Unit] =
          for {
            count <- Sync[F].delay(flyway.migrate())
            _ <- logger.info(show"Performed $count migration(s).")
          } yield ()
      }

}
