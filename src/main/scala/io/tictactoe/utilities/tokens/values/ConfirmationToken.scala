package io.tictactoe.utilities.tokens.values

import io.circe.{Decoder, Encoder}
import sttp.tapir.Codec
import sttp.tapir.Codec.PlainCodec
import io.circe.generic.extras.semiauto._

final case class ConfirmationToken(value: String) extends AnyVal

object ConfirmationToken {

  implicit val codec: PlainCodec[ConfirmationToken] = Codec.string.map(ConfirmationToken(_))(_.value)

  implicit val encoder: Encoder[ConfirmationToken] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[ConfirmationToken] = deriveUnwrappedDecoder

}
