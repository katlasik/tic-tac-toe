package io.tictactoe.documentation

import cats.Applicative
import cats.effect.{ContextShift, Sync}
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import cats.implicits._
import cats.data.NonEmptyList
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.routes.RoutingModule
import org.http4s.HttpRoutes
import sttp.tapir._
import sttp.tapir.server.http4s._
import cats.implicits._
import io.tictactoe.BuildInfo

trait Documentation[F[_]] {
  def routes(): HttpRoutes[F]
}

object Documentation {

  def documentation[F[_]: Applicative: Http4sServerOptions: Sync: ContextShift](body: String): HttpRoutes[F] =
    endpoint.get
      .in("docs")
      .out(header("Content-Type", "text/yaml"))
      .out(plainBody[String])
      .serverLogic(_ => body.pure[F].map(_.asRight[Unit]))
      .toRoutes

  def init[F[_]: Configuration: Sync: ContextShift: Applicative: Http4sServerOptions](
      router: RoutingModule[F],
      rest: RoutingModule[F]*
  ): Documentation[F] = {

    val endpoints = NonEmptyList.of(router, rest: _*).flatMap(_.router.endpoints)
    val yaml = endpoints.toList.toOpenAPI(BuildInfo.name, BuildInfo.version).toYaml

    () => documentation(yaml)
  }

}
