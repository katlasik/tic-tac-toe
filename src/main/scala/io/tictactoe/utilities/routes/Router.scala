package io.tictactoe.utilities.routes

import cats.data.NonEmptyList
import cats.effect.{ContextShift, Sync}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerOptions
import org.http4s.HttpRoutes
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s._
import cats.implicits._

trait Router[F[_]] {

  protected def serverEndpoints: NonEmptyList[ServerEndpoint[_, _, _, Nothing, F]]

  def routes(implicit serverOptions: Http4sServerOptions[F], fs: Sync[F], fcs: ContextShift[F]): HttpRoutes[F] =
    serverEndpoints.reduceMapK(_.toRoutes)

  def endpoints: NonEmptyList[Endpoint[_, _, _, Nothing]] = serverEndpoints.map(_.endpoint)

}
