package io.tictactoe.values

import java.util.UUID

import cats.{Eq, Show}
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import io.tictactoe.utilities.uuid.Id
import shapeless.the
import sttp.tapir.Codec.PlainCodec

final case class GameId(value: UUID) extends AnyVal

object GameId extends Id[GameId] {

  implicit val eq: Eq[GameId] = Eq.fromUniversalEquals
  implicit val show: Show[GameId] = Show.show(_.value.toString)

  implicit val codec: PlainCodec[GameId] = the[PlainCodec[UUID]].map(GameId(_))(_.value)

  implicit val encoder: Encoder[GameId] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[GameId] = deriveUnwrappedDecoder

}
