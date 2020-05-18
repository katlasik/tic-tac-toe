package io.tictactoe.values

import java.time.Instant

final case class EventTimestamp(value: Instant) extends AnyVal

object EventTimestamp {

  def unsafeFromString(str: String): EventTimestamp = EventTimestamp(Instant.parse(str))

}
