package io.tictactoe.values

import java.util.UUID

import cats.{Eq, Show}
import io.circe.{Decoder, Encoder}
import shapeless.the
import sttp.tapir.Codec.PlainCodec
import io.circe.generic.extras.semiauto._
import io.tictactoe.infrastructure.uuid.Id

final case class UserId(value: UUID) extends AnyVal

object UserId extends Id[UserId] {

  implicit val eq: Eq[UserId] = Eq.fromUniversalEquals
  implicit val show: Show[UserId] = Show.show(_.value.toString)

  implicit val codec: PlainCodec[UserId] = the[PlainCodec[UUID]].map(UserId(_))(_.value)

  implicit val encoder: Encoder[UserId] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[UserId] = deriveUnwrappedDecoder

}
