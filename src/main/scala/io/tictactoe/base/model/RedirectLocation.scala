package io.tictactoe.base.model

import cats.Show
import shapeless.the
import sttp.tapir.Codec.PlainCodec

final case class RedirectLocation(value: String) extends AnyVal

object RedirectLocation {
  implicit val codec: PlainCodec[RedirectLocation] = the[PlainCodec[String]].map(RedirectLocation(_))(_.value)

  implicit val show: Show[RedirectLocation] = Show.show(_.value)
}
