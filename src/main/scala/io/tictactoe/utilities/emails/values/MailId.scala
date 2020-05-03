package io.tictactoe.utilities.emails.values

import java.util.UUID

import cats.implicits._
import cats.kernel.Eq
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import io.tictactoe.utilities.uuid.Id

final case class MailId(value: UUID) extends AnyVal

object MailId extends Id[MailId] {

  implicit val eq: Eq[MailId] = Eq.by(_.value)

  implicit val encoder: Encoder[MailId] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[MailId] = deriveUnwrappedDecoder
}
