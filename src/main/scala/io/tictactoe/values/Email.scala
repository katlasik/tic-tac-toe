package io.tictactoe.values

import cats.data.ValidatedNel
import cats.implicits._
import io.circe.{Decoder, Encoder}
import io.tictactoe.utilities.validation.ValidationError
import mouse.all._
import sttp.tapir.{Codec, Validator}
import sttp.tapir.Codec.PlainCodec

final case class Email(value: String) extends AnyVal

object Email {

  private val EmailRegex = "^[^@]+@[^@]+$".r
  private val ValidatonMessage = "Email has wrong format."

  implicit val decoder: Decoder[Email] = Decoder[String].emap(Email.fromString(_).leftMap(_.reduce.msg).toEither)
  implicit val encoder: Encoder[Email] = Encoder[String].contramap(_.value)

  implicit val codec: PlainCodec[Email] = Codec.string
    .map(Email(_))(_.value)
    .validate(Validator.custom(e => EmailRegex.matches(e.value), ValidatonMessage))

  def fromString(value: String): ValidatedNel[ValidationError, Email] =
    EmailRegex
      .matches(value)
      .fold(
        new Email(value).validNel,
        ValidationError(ValidatonMessage).invalidNel
      )

}
