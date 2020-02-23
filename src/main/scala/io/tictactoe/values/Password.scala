package io.tictactoe.values

import cats.Eq
import cats.data.ValidatedNel
import cats.implicits._
import io.circe.{Decoder, Encoder}
import io.tictactoe.base.validation.ValidationError
import mouse.all._

final case class Password(value: String) extends AnyVal

object Password {

  private val RequiredSize = 6

  implicit val eq: Eq[Password] = Eq.fromUniversalEquals

  implicit val decoder: Decoder[Password] = Decoder[String].emap(Password.fromString(_).leftMap(_.reduce.msg).toEither)
  implicit val encoder: Encoder[Password] = Encoder[String].contramap(_.value)

  def fromString(value: String): ValidatedNel[ValidationError, Password] =
    (value.length >= RequiredSize)
      .fold(
        new Password(value).validNel[ValidationError],
        ValidationError(show"Password needs to have at least $RequiredSize characters.").invalidNel[Password]
      )

}
