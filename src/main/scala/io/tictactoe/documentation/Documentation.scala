package io.tictactoe.documentation

import cats.Applicative
import cats.effect.{ContextShift, Sync}
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import cats.data.NonEmptyList
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.routes.RoutingModule
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._
import io.tictactoe.BuildInfo
import sttp.tapir.swagger.http4s.SwaggerHttp4s

trait Documentation[F[_]] {
  def routes(): HttpRoutes[F]
}

object Documentation {

  def documentationRoutes[F[_]: Applicative: Http4sServerOptions: Sync: ContextShift](yaml: String): HttpRoutes[F] =
    new SwaggerHttp4s(yaml).routes

  def init[F[_]: Configuration: Sync: ContextShift: Applicative: Http4sServerOptions](
      router: RoutingModule[F],
      rest: RoutingModule[F]*
  ): Documentation[F] = {
    val endpoints = NonEmptyList.of(router, rest: _*).flatMap(_.router.endpoints)
    val yaml = endpoints.toList.toOpenAPI(BuildInfo.name, BuildInfo.version).toYaml
    () => documentationRoutes(yaml)
  }

}
