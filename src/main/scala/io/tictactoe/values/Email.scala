package io.tictactoe.values

import cats.Show
import cats.data.ValidatedNel
import cats.implicits._
import cats.kernel.Eq
import io.circe.{Decoder, Encoder}
import io.tictactoe.infrastructure.validation.ValidationError
import mouse.all._
import sttp.tapir.Codec
import sttp.tapir.Codec.PlainCodec

final case class Email(value: String) extends AnyVal

object Email {

  private val EmailRegex = "^[^@]+@[^@]+$".r

  implicit val eq: Eq[Email] = Eq.fromUniversalEquals
  implicit val show: Show[Email] = Show.show(_.value)

  implicit val decoder: Decoder[Email] = Decoder[String].emap(Email.fromString(_).leftMap(_.reduce.msg).toEither)
  implicit val encoder: Encoder[Email] = Encoder[String].contramap(_.value)

  implicit val codec: PlainCodec[Email] = Codec.string.map(Email(_))(_.value)

  def fromString(value: String): ValidatedNel[ValidationError, Email] =
    EmailRegex
      .matches(value)
      .fold(
        new Email(value).validNel,
        ValidationError("Email has wrong format.").invalidNel
      )

}
