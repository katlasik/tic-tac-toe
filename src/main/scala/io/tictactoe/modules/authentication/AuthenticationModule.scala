package io.tictactoe.modules.authentication

import cats.effect.{ContextShift, Sync}
import cats.implicits._
import io.tictactoe.modules.authentication.domain.services.{LiveAuthEmail, LiveAuthenticator, LivePasswordChanger, LiveRegistration}
import io.tictactoe.modules.authentication.infrastructure.effects.PasswordHasher
import io.tictactoe.modules.authentication.infrastructure.repositories.AuthRepository
import io.tictactoe.modules.authentication.infrastructure.routes.AuthenticationRouter
import io.tictactoe.modules.authentication.infrastructure.services.{Authenticator, PasswordChanger, Registration}
import io.tictactoe.modules.game.GameModule
import io.tictactoe.utilities.database.Database
import io.tictactoe.utilities.authentication.Authentication
import io.tictactoe.utilities.calendar.Calendar
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.emails.EmailSender
import io.tictactoe.utilities.events.EventBus
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.routes.RoutingModule
import io.tictactoe.utilities.tokens.TokenGenerator
import io.tictactoe.utilities.uuid.UUIDGenerator
import sttp.tapir.server.http4s.Http4sServerOptions

trait AuthenticationModule[F[_]] extends RoutingModule[F] {
  def authenticator: Authenticator[F]
  def passwordChanger: PasswordChanger[F]
  def registration: Registration[F]
  def router: AuthenticationRouter[F]
}

object AuthenticationModule {

  def live[F[_]: Sync: Logging: Configuration: EmailSender: Authentication: PasswordHasher: Database: EventBus: Calendar: TokenGenerator: UUIDGenerator: ContextShift: Http4sServerOptions](
      gameModule: GameModule[F]
  ): F[AuthenticationModule[F]] = {
    val authRepository = AuthRepository.postgresql

    for {
      e <- LiveAuthEmail.live
      a <- LiveAuthenticator.live(authRepository)
      ps <- LivePasswordChanger.live(e, authRepository)
      r <- LiveRegistration.live(e, authRepository, gameModule.invitationService)
    } yield
      new AuthenticationModule[F] {
        override def authenticator: Authenticator[F] = a

        override def passwordChanger: PasswordChanger[F] = ps

        override def registration: Registration[F] = r

        override def router: AuthenticationRouter[F] = new AuthenticationRouter[F](r, a, ps)
      }
  }
}
