package io.tictactoe.game.values

import java.util.UUID

import cats.{Eq, Show}
import io.circe.{Decoder, Encoder}
import io.tictactoe.infrastructure.uuid.Id
import sttp.tapir.Codec.PlainCodec
import io.circe.generic.extras.semiauto._
import shapeless.the

final case class GameId(value: UUID) extends AnyVal

object GameId extends Id[GameId] {

  implicit val eq: Eq[GameId] = Eq.fromUniversalEquals
  implicit val show: Show[GameId] = Show.show(_.value.toString)

  implicit val codec: PlainCodec[GameId] = the[PlainCodec[UUID]].map(GameId(_))(_.value)

  implicit val encoder: Encoder[GameId] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[GameId] = deriveUnwrappedDecoder

}
