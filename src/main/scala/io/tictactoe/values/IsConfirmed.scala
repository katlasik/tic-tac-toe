package io.tictactoe.values

import cats.Eq
import doobie.util.meta.Meta
import mouse.all._
import cats.implicits._

sealed trait IsConfirmed extends Product with Serializable {
  val value: Boolean

  def fold[B](ifNotConfirmed: => B)(ifConfirmed: => B): B =
    if (value) ifConfirmed else ifNotConfirmed
}

case object Confirmed extends IsConfirmed {
  val value = true
}

case object Unconfirmed extends IsConfirmed {
  val value = false
}

object IsConfirmed {

  implicit val eq: Eq[IsConfirmed] = Eq.by(_.value)
  implicit val meta: Meta[IsConfirmed] = Meta[Boolean].timap(_.fold(Confirmed, Unconfirmed))(_.value)

}
