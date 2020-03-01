package io.tictactoe.authentication.services

import cats.effect.Sync
import io.tictactoe.authentication.model.{RegistrationRequest, RegistrationResult, User, ValidatedRegistrationRequest}
import io.tictactoe.base.uuid.UUIDGenerator
import cats.implicits._
import io.tictactoe.authentication.errors.{EmailAlreadyExists, UsernameAlreadyExists}
import io.tictactoe.authentication.events.UserRegisteredEvent
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.base.logging.Logging
import io.tictactoe.base.validation.Validator._
import io.tictactoe.calendar.Calendar
import io.tictactoe.events.bus.EventBus
import io.tictactoe.values.{No, UserId}

trait Registration[F[_]] {
  def register(request: RegistrationRequest): F[RegistrationResult]
}

object Registration {

  def apply[F[_]](implicit ev: Registration[F]): Registration[F] = ev

  def live[F[_]: PasswordHasher: UUIDGenerator: Sync: AuthRepository: Logging: EventBus: Calendar]: Registration[F] = new Registration[F] {
    override def register(request: RegistrationRequest): F[RegistrationResult] =
      for {
        logger <- Logging[F].create[Registration[F]]
        ValidatedRegistrationRequest(name, password, email) <- request.validate[F]
        emailExists <- AuthRepository[F].existsByEmail(email)
        _ <- Sync[F].whenA(emailExists)(Sync[F].raiseError(EmailAlreadyExists))
        usernameExists <- AuthRepository[F].existsByName(name)
        _ <- Sync[F].whenA(usernameExists)(Sync[F].raiseError(UsernameAlreadyExists))
        hash <- PasswordHasher[F].hash(password)
        id <- UserId.next[F]
        user <- AuthRepository[F].save(User(id, name, hash, email, No))
        _ <- logger.info(show"New user with id = $id was created.")
        _ <- EventBus[F].publishF(UserRegisteredEvent.create[F](user))
      } yield RegistrationResult(id)
  }

}
