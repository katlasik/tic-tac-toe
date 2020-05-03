package io.tictactoe.server

import cats.Parallel
import cats.effect.{ConcurrentEffect, ContextShift, Sync, Timer}
import fs2.Stream
import io.tictactoe.utilities.database.{Database, Migrator}
import io.tictactoe.utilities.logging.Logging
import org.http4s.server.blaze.BlazeServerBuilder
import cats.implicits._
import io.tictactoe.authentication.AuthenticationModule
import io.tictactoe.authentication.infrastructure.effects.PasswordHasher
import io.tictactoe.utilities.events.EventBus
import io.tictactoe.utilities.tokens.TokenGenerator
import io.tictactoe.utilities.uuid.UUIDGenerator
import io.tictactoe.utilities.calendar.Calendar
import io.tictactoe.scheduler.ApplicationScheduler
import io.tictactoe.utilities.scheduler.Scheduler
import io.tictactoe.events.ApplicationEventHandler
import io.tictactoe.game.GameModule
import io.tictactoe.users.UserModule
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.emails.EmailSender
import io.tictactoe.utilities.authentication.Authentication

object Server {

  def stream[F[_]: ConcurrentEffect: Sync: ContextShift: Parallel](implicit T: Timer[F]): Stream[F, Nothing] = {

    implicit val uuidGenerator: UUIDGenerator[F] = UUIDGenerator.live[F]
    implicit val passwordHasher: PasswordHasher[F] = PasswordHasher.bcrypt[F]
    implicit val logging: Logging[F] = Logging.live[F]
    implicit val calendar: Calendar[F] = Calendar.live[F]

    for {
      implicit0(authentication: Authentication[F]) <- Stream.eval(Authentication.live[F])
      implicit0(scheduler: Scheduler[F]) <- Stream.eval(Scheduler.live[F])
      implicit0(configuration: Configuration[F]) <- Stream.eval(Configuration.load[F])
      implicit0(database: Database[F]) <- Stream.resource(Database.hikari[F])
      implicit0(emailSender: EmailSender[F]) <- Stream.eval(EmailSender.live[F])
      implicit0(tokenGenerator: TokenGenerator[F]) <- Stream.eval(TokenGenerator.live[F])
      implicit0(eventBus: EventBus[F]) <- Stream.eval(EventBus.create())
      userModule = UserModule.live
      gameModule <- Stream.eval(GameModule.live[F](userModule))
      authModule <- Stream.eval(AuthenticationModule.live[F](gameModule))
      handler <- Stream.eval(ApplicationEventHandler.live[F](authModule))
      _ <- Stream.resource(handler.start)
      _ <- Stream.resource(ApplicationScheduler.live[F].start())
      _ <- Stream.eval(Migrator.init[F].flatMap(_.migrate))
      config <- Stream.eval(configuration.access()).map(_.server)
      app <- App.create(userModule, gameModule, authModule)
      exitCode <- BlazeServerBuilder[F]
        .bindHttp(config.port, config.host)
        .withHttpApp(app)
        .serve
    } yield exitCode
  }.drain
}
