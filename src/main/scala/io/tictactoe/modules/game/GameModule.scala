package io.tictactoe.modules.game

import cats.effect.{ContextShift, Sync}
import io.tictactoe.modules.game.domain.services.{LiveGameInvitationService, LiveGameService}
import io.tictactoe.modules.game.infrastructure.repositories.{GameRepository, InvitationRepository}
import io.tictactoe.modules.game.infrastructure.services.{GameInvitationService, GameService}
import io.tictactoe.utilities.database.Database
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.tokens.TokenGenerator
import io.tictactoe.utilities.uuid.UUIDGenerator
import cats.implicits._
import io.tictactoe.modules.game.infrastructure.emails.InvitationEmail
import io.tictactoe.modules.game.infrastructure.routes.GameRouter
import io.tictactoe.modules.users.UserModule
import io.tictactoe.utilities.authentication.Authentication
import io.tictactoe.utilities.calendar.Calendar
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.emails.EmailSender
import io.tictactoe.utilities.events.EventBus
import io.tictactoe.utilities.random.RandomPicker
import io.tictactoe.utilities.routes.RoutingModule

trait GameModule[F[_]] extends RoutingModule[F] {
  def invitationService: GameInvitationService[F]

  def gameService: GameService[F]

  def router: GameRouter[F]
}

object GameModule {

  def live[F[_]: Sync: RandomPicker: TokenGenerator: UUIDGenerator: Logging: Database: Configuration: EmailSender: ContextShift: Authentication: EventBus: Calendar](
      userModule: UserModule[F]
  ): F[GameModule[F]] = {
    val ir = InvitationRepository.postgresql
    for {
      ie <- InvitationEmail.live
      is <- LiveGameInvitationService.live(ir, ie, userModule.userService)
      r = GameRepository.live
    } yield
      new GameModule[F] {
        override def invitationService: GameInvitationService[F] = is

        override def gameService: GameService[F] = LiveGameService.live(r)

        override def router: GameRouter[F] = new GameRouter[F](is)
      }
  }

}
