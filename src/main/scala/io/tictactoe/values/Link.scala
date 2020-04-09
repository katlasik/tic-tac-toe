package io.tictactoe.values

import cats.Show

final case class Link (value: String)

object Link {

  implicit val show: Show[Link] = Show.show(_.value)

}
