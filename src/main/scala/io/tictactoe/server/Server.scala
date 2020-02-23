package io.tictactoe.server

import cats.Parallel
import cats.effect.{ConcurrentEffect, ContextShift, Sync, Timer}
import fs2.Stream
import io.tictactoe.configuration.Configuration
import io.tictactoe.database.{Database, Migrator}
import io.tictactoe.base.logging.Logging
import io.tictactoe.routes.{PublicRouter, SecuredRouter}
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.implicits._
import cats.implicits._
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.authentication.services.{Authentication, PasswordHasher, Registration}
import io.tictactoe.base.errors.ErrorTranslator
import io.tictactoe.base.uuid.UUIDGenerator
import io.tictactoe.documentation.DocsGenerator
import io.tictactoe.users.repositories.UserRepository
import io.tictactoe.users.services.UserService

object Server {

  def app[F[_]: ContextShift: Sync: ConcurrentEffect: Registration: Authentication: UserService: AuthRepository: Logging]: HttpApp[F] = {
    val httpApp = (PublicRouter.routes[F] <+> SecuredRouter.routes[F]).orNotFound.map(ErrorTranslator.handle)

    Logger.httpApp(logHeaders = false, logBody = true)(httpApp)
  }

  def stream[F[_]: ConcurrentEffect: Sync: ContextShift: Parallel](implicit T: Timer[F]): Stream[F, Nothing] = {

    implicit val uuidGenerator: UUIDGenerator[F] = UUIDGenerator.live[F]
    implicit val passwordHasher: PasswordHasher[F] = PasswordHasher.bcrypt[F]
    implicit val logging: Logging[F] = Logging.live[F]

    for {
      implicit0(configuration: Configuration[F]) <- Stream.eval(Configuration.load[F])
      implicit0(database: Database[F]) <- Stream.resource(Database.hikari[F])
      implicit0(authRepository: AuthRepository[F]) = AuthRepository.postgresql[F]
      implicit0(userRepository: UserRepository[F]) = UserRepository.postgresql[F]
      implicit0(registration: Registration[F]) = Registration.live[F]
      implicit0(authentication: Authentication[F]) <- Stream.eval(Authentication.live[F])
      implicit0(userService: UserService[F]) = UserService.live[F]
      _ <- Stream.eval(Migrator.init[F].flatMap(_.migrate))
      _ <- Stream.eval(DocsGenerator.init[F].generate())
      server <- Stream.eval(configuration.access()).map(_.server)
      exitCode <- BlazeServerBuilder[F]
        .bindHttp(server.port, server.host)
        .withHttpApp(app)
        .serve
    } yield exitCode
  }.drain
}
