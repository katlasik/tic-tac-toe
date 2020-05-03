package io.tictactoe.testutils.mocks

import java.time.LocalDateTime

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.modules.game.infrastructure.repositories.InvitationRepository
import io.tictactoe.modules.game.model.{AcceptedGameInvitation, CancelledGameInvitation, GameInvitation, PendingGameInvitation, RejectedGameInvitation}
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState
import cats.implicits._
import henkan.convert.Syntax._
import io.tictactoe.errors.ResourceNotFound
import io.tictactoe.values.{GameId, UserId}

object InMemoryInvitationRepository {

  def inMemory: InvitationRepository[TestAppState] = new InvitationRepository[TestAppState] {

    override def save(invitation: PendingGameInvitation): TestAppState[GameInvitation] = StateT { data: TestAppData =>
      IO.pure((data.copy(invitations = invitation :: data.invitations), invitation))
    }

    override def get(gameId: GameId): TestAppState[Option[GameInvitation]] = StateT { data: TestAppData =>
      IO.pure((data, data.invitations.find(_.id === gameId)))
    }

    def modify[T <: GameInvitation](gameId: GameId, f: (PendingGameInvitation, LocalDateTime) => T): TestAppState[T] = StateT {
      data: TestAppData =>
        data.dates match {
          case head :: tail =>
            data.invitations.collectFirst {
              case invitation: PendingGameInvitation if invitation.id === gameId => f(invitation, head)
            } match {
              case Some(invitation) =>
                IO.pure((data.copy(invitations = invitation :: data.invitations.filter(_.id =!= gameId), dates = tail), invitation))
              case None =>
                IO.raiseError(ResourceNotFound)
            }
          case _ => IO.raiseError(new IllegalArgumentException("The dates list is empty."))
        }
    }

    override def acceptAndSetGuest(gameId: GameId, guestId: UserId): TestAppState[AcceptedGameInvitation] =
      modify(gameId, { case (invitation, date) => invitation.to[AcceptedGameInvitation].set(acceptedOn = date, guestId = guestId) })

    override def cancel(gameId: GameId): TestAppState[CancelledGameInvitation] =
      modify(gameId, { case (invitation, date) => invitation.to[CancelledGameInvitation].set(cancelledOn = date) })

    override def reject(gameId: GameId): TestAppState[RejectedGameInvitation] =
      modify(gameId, { case (invitation, date) => invitation.to[RejectedGameInvitation].set(rejectedOn = date) })

    override def accept(gameId: GameId): TestAppState[AcceptedGameInvitation] =
      modify(gameId, {
        case (invitation, date) => invitation.to[AcceptedGameInvitation].set(acceptedOn = date, guestId = invitation.guestId.get)
      })
  }

}
