package io.tictactoe.values

import doobie.util.meta.Meta
import mouse.all._

sealed trait IsConfirmed extends Product with Serializable{
  val value: Boolean
}

case object Yes extends IsConfirmed {
  val value = true
}

case object No extends IsConfirmed {
  val value = false
}

object IsConfirmed {

  implicit val meta: Meta[IsConfirmed] = Meta[Boolean].timap(_.fold(Yes, No))(_.value)

}
