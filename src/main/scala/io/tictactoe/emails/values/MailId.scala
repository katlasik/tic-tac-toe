package io.tictactoe.emails.values

import java.util.UUID

import cats.Functor
import cats.implicits._
import cats.kernel.Eq
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import io.tictactoe.infrastructure.uuid.UUIDGenerator

final case class MailId(value: UUID) extends AnyVal

object MailId {

  def next[F[_]: UUIDGenerator: Functor]: F[MailId] =
    for {
      id <- UUIDGenerator[F].next()
    } yield MailId(id)

  def fromString(value: String): MailId = MailId(UUID.fromString(value))

  implicit val eq: Eq[MailId] = Eq.by(_.value)

  implicit val encoder: Encoder[MailId] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[MailId] = deriveUnwrappedDecoder
}
