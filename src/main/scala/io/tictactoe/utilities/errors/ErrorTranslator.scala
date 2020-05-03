package io.tictactoe.utilities.errors

import cats.effect.ConcurrentEffect
import io.tictactoe.errors.ErrorView
import org.http4s.{Response, Status}
import org.http4s.util.CaseInsensitiveString
import org.http4s.circe.CirceEntityCodec._
import io.circe.generic.auto._

object ErrorTranslator {

  val ContentType = "Content-Type"
  val ApplicationJson = "application/json"

  private def translate[F[_]: ConcurrentEffect](r: Response[F]): Response[F] =
    r.headers.get(CaseInsensitiveString(ContentType)).map(_.value) match {
      case Some(ApplicationJson) => r
      case _                     => r.withEntity(r.bodyAsText.map(ErrorView(_)))
    }

  def handle[F[_]: ConcurrentEffect]: PartialFunction[Response[F], Response[F]] = {
    case Status.ClientError(r) => translate(r)
    case r                     => r
  }

}
