package io.tictactoe.authentication.services

import cats.effect.Sync
import io.tictactoe.authentication.model.{RegistrationRequest, RegistrationResult, User, ValidatedRegistrationRequest}
import io.tictactoe.infrastructure.uuid.UUIDGenerator
import cats.implicits._
import io.tictactoe.authentication.errors.{EmailAlreadyExists, IllegalConfirmationToken, ResourceNotFound, UsernameAlreadyExists}
import io.tictactoe.authentication.events.UserRegisteredEvent
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.game.services.GameInvitationService
import io.tictactoe.game.values.GameId
import io.tictactoe.infrastructure.events.EventBus
import io.tictactoe.infrastructure.logging.Logging
import io.tictactoe.infrastructure.model.RedirectLocation
import io.tictactoe.infrastructure.tokens.TokenGenerator
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken
import io.tictactoe.infrastructure.validation.Validator._
import io.tictactoe.infrastructure.calendar.Calendar
import io.tictactoe.infrastructure.configuration.Configuration
import io.tictactoe.values.{Confirmed, Email, IsConfirmed, Password, Unconfirmed, UserId, Username}
import io.tictactoe.infrastructure.syntax._

trait Registration[F[_]] {
  def register(request: RegistrationRequest): F[RegistrationResult]

  def confirm(token: ConfirmationToken, id: UserId): F[RedirectLocation]

  def resendEmail(email: Email): F[Unit]
}

object Registration {

  def apply[F[_]](implicit ev: Registration[F]): Registration[F] = ev

  def live[
      F[_]: AuthEmail: GameInvitationService: Configuration: PasswordHasher: UUIDGenerator: Sync: AuthRepository: Logging: EventBus: Calendar: TokenGenerator]
    : F[Registration[F]] =
    for {
      logger <- Logging[F].create[Registration[F]]
    } yield
      new Registration[F] {

        def createUser(name: Username, password: Password, email: Email, isConfirmed: IsConfirmed): F[User] =
          for {
            _ <- EmailAlreadyExists.throwWhenM(AuthRepository[F].existsByEmail(email))
            _ <- UsernameAlreadyExists.throwWhenM(AuthRepository[F].existsByName(name))
            hash <- PasswordHasher[F].hash(password)
            id <- UserId.next[F]
            token <- isConfirmed.fold(TokenGenerator[F].generate.map(_.some))(Sync[F].pure(none[ConfirmationToken]))
            user <- AuthRepository[F].save(User(id, name, hash, email, isConfirmed, token, None))
          } yield user

        def registerWithInvitation(name: Username, password: Password, email: Email, data: (ConfirmationToken, GameId)): F[User] =
          data match {
            case (invitationToken, gameId: GameId) =>
              for {
                isConfirmed <- GameInvitationService[F].get(gameId).flatMap {
                  case Some(game) if game.token.contains(invitationToken) => Confirmed.pure[F].widen[IsConfirmed]
                  case _                                                  => Unconfirmed.pure[F].widen[IsConfirmed]
                }
                user <- createUser(name, password, email, isConfirmed)
                _ <- GameInvitationService[F].acceptInvitationAndSetInvitee(gameId, user.id).whenA(isConfirmed === Confirmed)
              } yield user
          }

        override def register(request: RegistrationRequest): F[RegistrationResult] =
          for {
            ValidatedRegistrationRequest(name, password, email, invitationData) <- request.validate[F]
            user <- invitationData.fold(createUser(name, password, email, Unconfirmed))(registerWithInvitation(name, password, email, _))
            _ <- logger.info(show"New user with id = ${user.id} was created.")
            _ <- EventBus[F].publishF(UserRegisteredEvent.create[F](user))
          } yield RegistrationResult(user.id)

        override def confirm(token: ConfirmationToken, id: UserId): F[RedirectLocation] =
          for {
            maybeUser <- AuthRepository[F].getById(id)
            result <- maybeUser match {
              case Some(user) if user.registrationConfirmationToken.contains(token) =>
                for {
                  _ <- AuthRepository[F].confirm(user)
                  redirectUrl <- Configuration[F].access().map(_.registration.confirmationRedirect)
                  _ <- logger.info(show"User with id = ${user.id} confirmed account. Redirectring to $redirectUrl.")
                } yield redirectUrl
              case _ => Sync[F].raiseError(IllegalConfirmationToken)
            }
          } yield result

        override def resendEmail(email: Email): F[Unit] =
          for {
            _ <- logger.info(show"Sending of new registration confirmation email requested by $email.")
            user <- AuthRepository[F].getByEmail(email).throwIfEmpty(ResourceNotFound)
            _ <- ResourceNotFound.throwWhen(user.isConfirmed === Confirmed)
            token <- TokenGenerator[F].generate
            _ <- AuthRepository[F].updateRegistrationConfirmationToken(user.id, token)
            _ <- AuthEmail[F].sendRegistrationConfirmation(email, user.username, user.id, token)
          } yield ()
      }

}
