package io.tictactoe.testutils.mocks

import cats.data.StateT
import cats.effect.IO
import io.tictactoe.game.infrastructure.repositories.InvitationRepository
import io.tictactoe.game.model.GameInvitation
import io.tictactoe.testutils.TestAppData
import io.tictactoe.testutils.TestAppData.TestAppState

object InMemoryInvitationRepository {

  def inMemory: InvitationRepository[TestAppState] = new InvitationRepository[TestAppState] {

    override def save(invitation: GameInvitation): TestAppState[GameInvitation] = StateT { data: TestAppData =>
      IO.pure((data.copy(invitations = invitation :: data.invitations), invitation))
    }

  }

}
