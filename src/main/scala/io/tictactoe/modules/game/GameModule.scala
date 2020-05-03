package io.tictactoe.modules.game

import cats.effect.{ContextShift, Sync}
import io.tictactoe.modules.game.domain.services.LiveGameInvitationService
import io.tictactoe.modules.game.infrastructure.repositories.InvitationRepository
import io.tictactoe.modules.game.infrastructure.services.GameInvitationService
import io.tictactoe.utilities.database.Database
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.tokens.TokenGenerator
import io.tictactoe.utilities.uuid.UUIDGenerator
import cats.implicits._
import io.tictactoe.modules.game.infrastructure.emails.InvitationEmail
import io.tictactoe.modules.game.infrastructure.routes.GameRouter
import io.tictactoe.modules.users.UserModule
import io.tictactoe.utilities.authentication.Authentication
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.emails.EmailSender
import io.tictactoe.utilities.routes.RoutingModule
import sttp.tapir.server.http4s.Http4sServerOptions

trait GameModule[F[_]] extends RoutingModule[F] {
  def invitationService: GameInvitationService[F]

  def router: GameRouter[F]
}

object GameModule {

  def live[F[_]: Sync: TokenGenerator: UUIDGenerator: Logging: Database: Configuration: EmailSender: ContextShift: Http4sServerOptions: Authentication](
      userModule: UserModule[F]
  ): F[GameModule[F]] = {
    val ir = InvitationRepository.postgresql
    for {
      ie <- InvitationEmail.live
      is <- LiveGameInvitationService.live(ir, ie, userModule.userService)
    } yield
      new GameModule[F] {
        override def invitationService: GameInvitationService[F] = is

        override def router: GameRouter[F] = new GameRouter[F](is)
      }
  }

}
