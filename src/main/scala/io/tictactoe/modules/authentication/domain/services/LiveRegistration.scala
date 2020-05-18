package io.tictactoe.modules.authentication.domain.services

import cats.effect.Sync
import io.tictactoe.modules.authentication.model.{RawRegistrationRequest, RegistrationRequest, RegistrationResult, User}
import io.tictactoe.utilities.uuid.UUIDGenerator
import cats.implicits._
import io.tictactoe.modules.authentication.errors.{EmailAlreadyExists, IllegalConfirmationToken, UsernameAlreadyExists}
import io.tictactoe.modules.authentication.infrastructure.effects.PasswordHasher
import io.tictactoe.modules.authentication.infrastructure.repositories.AuthRepository
import io.tictactoe.modules.authentication.infrastructure.services.{AuthEmail, Registration}
import io.tictactoe.errors.ResourceNotFound
import io.tictactoe.events.model.authentication.UserRegisteredEvent
import io.tictactoe.modules.game.infrastructure.services.GameInvitationService
import io.tictactoe.utilities.events.EventBus
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.tokens.TokenGenerator
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.utilities.validation.Validator._
import io.tictactoe.utilities.calendar.Calendar
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.values.{Confirmed, Email, GameId, IsConfirmed, Password, Unconfirmed, UserId, Username}
import io.tictactoe.implicits._

object LiveRegistration {

  def live[F[_]: Configuration: PasswordHasher: UUIDGenerator: Sync: Logging: EventBus: Calendar: TokenGenerator](
      authEmail: AuthEmail[F],
      authRepository: AuthRepository[F],
      invitationService: GameInvitationService[F]
  ): F[Registration[F]] =
    for {
      logger <- Logging[F].create[Registration[F]]
    } yield
      new Registration[F] {

        def createUser(name: Username, password: Password, email: Email, isConfirmed: IsConfirmed): F[User] =
          for {
            _ <- EmailAlreadyExists.throwWhenM(authRepository.existsByEmail(email))
            _ <- UsernameAlreadyExists.throwWhenM(authRepository.existsByName(name))
            hash <- PasswordHasher[F].hash(password)
            id <- UserId.next[F]
            token <- isConfirmed.fold(TokenGenerator[F].generate.map(_.some))(Sync[F].pure(none[ConfirmationToken]))
            user <- authRepository.save(User(id, name, hash, email, isConfirmed, token, None))
          } yield user

        def registerWithInvitation(name: Username, password: Password, email: Email, data: (ConfirmationToken, GameId)): F[User] =
          data match {
            case (invitationToken, gameId: GameId) =>
              for {
                isConfirmed <- invitationService.get(gameId).map {
                  case Some(game) if game.token.contains(invitationToken) => Confirmed
                  case _                                                  => Unconfirmed
                }
                user <- createUser(name, password, email, isConfirmed)
                _ <- invitationService.acceptInvitationAndSetInvitee(gameId, user.id).whenA(isConfirmed === Confirmed)
              } yield user
          }

        override def register(request: RawRegistrationRequest): F[RegistrationResult] =
          for {
            RegistrationRequest(name, password, email, invitationData) <- request.validate[F]
            user <- invitationData.fold(createUser(name, password, email, Unconfirmed))(registerWithInvitation(name, password, email, _))
            _ <- logger.info(show"New user with id = ${user.id} was created.")
            _ <- EventBus[F].publishF(UserRegisteredEvent.create[F](user))
          } yield RegistrationResult(user.id)

        override def confirm(token: ConfirmationToken, id: UserId): F[Unit] =
          for {
            maybeUser <- authRepository.getById(id)
            result <- maybeUser match {
              case Some(user) if user.registrationConfirmationToken.contains(token) =>
                for {
                  _ <- authRepository.confirm(user)
                  _ <- logger.info(show"User with id = ${user.id} confirmed account.")
                } yield ()
              case _ => Sync[F].raiseError(IllegalConfirmationToken)
            }
          } yield result

        override def resendEmail(email: Email): F[Unit] =
          for {
            _ <- logger.info(show"Sending of new registration confirmation email requested by $email.")
            user <- authRepository.getByEmail(email).throwIfEmpty(ResourceNotFound)
            _ <- ResourceNotFound.throwWhen(user.isConfirmed === Confirmed)
            token <- TokenGenerator[F].generate
            _ <- authRepository.updateRegistrationConfirmationToken(user.id, token)
            _ <- authEmail.sendRegistrationConfirmation(email, user.username, user.id, token)
          } yield ()

        override def sendRegistrationConfirmationMail(email: Email, username: Username, userId: UserId, token: ConfirmationToken): F[Unit] =
          authEmail.sendRegistrationConfirmation(email, username, userId, token)
      }

}
