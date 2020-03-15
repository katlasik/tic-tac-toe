package io.tictactoe.authentication.values

import cats.Eq
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import shapeless.the
import sttp.tapir.Codec.PlainCodec

final case class AuthToken(value: String) extends AnyVal

object AuthToken {
  implicit val eq: Eq[AuthToken] = Eq.fromUniversalEquals
  implicit val codec: PlainCodec[AuthToken] = the[PlainCodec[String]].map(AuthToken(_))(_.value)

  implicit val encoder: Encoder[AuthToken] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[AuthToken] = deriveUnwrappedDecoder
}
