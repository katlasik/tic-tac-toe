package io.tictactoe.utilities.database

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import cats.implicits._
import io.tictactoe.utilities.configuration.Configuration

trait Database[F[_]] {
  def transactor(): HikariTransactor[F]
}

object Database {

  def apply[F[_]](implicit ev: Database[F]): Database[F] = ev

  def hikari[F[_]: Configuration: Async: ContextShift: Sync]: Resource[F, Database[F]] =
    for {
      config <- Resource.liftF(Configuration[F].access().map(_.db))
      connectExecutionContext <- ExecutionContexts.fixedThreadPool[F](32)
      transactExecutionContext <- ExecutionContexts.cachedThreadPool[F]
      db <- Resource.liftF(Sync[F].fromEither(DbUrlParser.parse(config.databaseUrl)))
      hikari <- HikariTransactor.newHikariTransactor(
        config.driver,
        db.jdbcUrl,
        db.username,
        db.password,
        transactExecutionContext,
        Blocker.liftExecutionContext(connectExecutionContext)
      )
    } yield
      new Database[F] {
        override val transactor: HikariTransactor[F] = hikari
      }

}
