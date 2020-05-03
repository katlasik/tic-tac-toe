package io.tictactoe.values

import cats.{Eq, Show}
import cats.data.ValidatedNel
import cats.implicits._
import io.circe.{Decoder, Encoder}
import io.tictactoe.utilities.validation.ValidationError
import mouse.all._

final case class Username(value: String) extends AnyVal

object Username {

  private val AllowedCharacters = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ List('_', ' ', '+', '-', '?')
  private val MinSize = 2

  implicit val eq: Eq[Username] = Eq.fromUniversalEquals
  implicit val show: Show[Username] = Show.show(_.value)

  implicit val decoder: Decoder[Username] = Decoder[String].emap(Username.fromString(_).leftMap(_.reduce.msg).toEither)
  implicit val encoder: Encoder[Username] = Encoder[String].contramap(_.value)

  def fromString(value: String): ValidatedNel[ValidationError, Username] =
    (value.length >= MinSize)
      .fold(
        new Username(value).validNel,
        ValidationError(show"Username length must be at least $MinSize.").invalidNel
      )
      .andThen(
        _ =>
          value
            .forall(AllowedCharacters.contains)
            .fold(
              new Username(value).validNel,
              ValidationError("Username has illegal characters.").invalidNel
          )
      )

}
