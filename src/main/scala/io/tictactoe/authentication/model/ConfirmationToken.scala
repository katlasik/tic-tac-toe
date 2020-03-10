package io.tictactoe.authentication.model

import cats.{Eq, Show}
import sttp.tapir.Codec
import sttp.tapir.Codec.PlainCodec

final case class ConfirmationToken(value: String) extends AnyVal

object ConfirmationToken {

  implicit val show: Show[ConfirmationToken] = Show.show(_.value)

  implicit val eq: Eq[ConfirmationToken] = Eq.fromUniversalEquals

  implicit val codec: PlainCodec[ConfirmationToken] = Codec.stringPlainCodecUtf8.map(ConfirmationToken(_))(_.value)

}
