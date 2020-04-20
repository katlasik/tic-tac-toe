package io.tictactoe.game.model

import enumeratum._
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

sealed trait GameInvitationStatus extends EnumEntry

object GameInvitationStatus
    extends Enum[GameInvitationStatus]
    with CirceEnum[GameInvitationStatus]
    with CatsEnum[GameInvitationStatus]
    with TapirCodecEnumeratum {

  case object Pending extends GameInvitationStatus
  case object Accepted extends GameInvitationStatus
  case object Cancelled extends GameInvitationStatus
  case object Rejected extends GameInvitationStatus

  override def values: IndexedSeq[GameInvitationStatus] = findValues
}
