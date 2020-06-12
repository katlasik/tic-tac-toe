package io.tictactoe.server

import cats.data.NonEmptyList
import cats.effect.{ConcurrentEffect, ContextShift, Sync}
import io.tictactoe.documentation.Documentation
import io.tictactoe.utilities.errors.ErrorTranslator
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.routes.RoutingModule
import org.http4s.HttpApp
import org.http4s.server.middleware.Logger
import cats.implicits._
import io.tictactoe.utilities.configuration.Configuration
import org.http4s.implicits._

object App {

  def create[F[_]: Configuration: ContextShift: Sync: ConcurrentEffect: Logging](first: RoutingModule[F], rest: RoutingModule[F]*): HttpApp[F] = {
    val documentation = Documentation.init[F](first, rest: _*)
    val routes = NonEmptyList.of(first, rest: _*).map(_.router.routes) ::: NonEmptyList.one(documentation.routes())
    val httpApp = routes.reduceK.orNotFound.map(ErrorTranslator.handle)
    Logger.httpApp(logHeaders = true, logBody = true)(httpApp)
  }

}
