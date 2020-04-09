package io.tictactoe.game.values

import java.util.UUID

import cats.{Eq, Functor, Show}
import io.circe.{Decoder, Encoder}
import io.tictactoe.infrastructure.uuid.UUIDGenerator
import shapeless.the
import sttp.tapir.Codec.PlainCodec
import io.circe.generic.extras.semiauto._
import cats.implicits._

final case class GameId(value: UUID) extends AnyVal

object GameId {
  def next[F[_]: UUIDGenerator: Functor]: F[GameId] =
    for {
      id <- UUIDGenerator[F].next()
    } yield GameId(id)

  def unsafeFromString(value: String): GameId = GameId(UUID.fromString(value))

  implicit val eq: Eq[GameId] = Eq.fromUniversalEquals
  implicit val show: Show[GameId] = Show.show(_.value.toString)

  implicit val codec: PlainCodec[GameId] = the[PlainCodec[UUID]].map(GameId(_))(_.value)

  implicit val encoder: Encoder[GameId] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[GameId] = deriveUnwrappedDecoder

}
