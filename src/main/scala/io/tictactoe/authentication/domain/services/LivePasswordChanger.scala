package io.tictactoe.authentication.domain.services

import cats.effect.Sync
import io.tictactoe.authentication.infrastructure.repositories.AuthRepository
import io.tictactoe.values.{Confirmed, Email, Username}
import cats.implicits._
import io.tictactoe.authentication.errors.IllegalConfirmationToken
import io.tictactoe.authentication.infrastructure.effects.PasswordHasher
import io.tictactoe.authentication.model.{PasswordChangeRequest, User}
import io.tictactoe.authentication.infrastructure.services.{AuthEmail, PasswordChanger}
import io.tictactoe.events.model.authentication.PasswordChangedEvent
import io.tictactoe.utilities.events.EventBus
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.tokens.TokenGenerator
import io.tictactoe.utilities.syntax._
import io.tictactoe.utilities.uuid.UUIDGenerator
import io.tictactoe.utilities.calendar.Calendar

object LivePasswordChanger {

  def live[F[_]: Sync: TokenGenerator: Logging: PasswordHasher: EventBus: Calendar: UUIDGenerator](authEmail: AuthEmail[F], authRepository: AuthRepository[F])
    : F[PasswordChanger[F]] =
    for {
      logger <- Logging[F].create[PasswordChanger[F]]
    } yield
      new PasswordChanger[F] {

        override def request(email: Email): F[Unit] =
          for {
            user <- authRepository.getByEmail(email)
            _ <- user match {
              case Some(User(id, username, _, _, Confirmed, _, _)) =>
                for {
                  token <- TokenGenerator[F].generate
                  _ <- logger.info(show"Sending of password reset mail requested for user with id = $id and email = $email.")
                  _ <- authRepository.updatePasswordResetToken(id, token)
                  _ <- authEmail.sendPasswordChangeRequest(email, username, id, token)
                } yield ()
              case _ => logger.info(show"No confirmed user with mail $email found in database, sending no email.")
            }
          } yield ()

        override def changePassword(request: PasswordChangeRequest): F[Unit] = request match {
          case PasswordChangeRequest(userId, token, newPassword) =>
            for {
              user <- authRepository.getById(userId).throwIfEmptyFilter(IllegalConfirmationToken)(_.passwordResetToken.contains(token))
              hash <- PasswordHasher[F].hash(newPassword)
              _ <- authRepository.updateHash(userId, hash)
              _ <- logger.info(show"Password of user with id $userId was changed.")
              _ <- EventBus[F].publishF(PasswordChangedEvent.create(user))
            } yield ()
        }

        override def sendPasswordChangedNotification(email: Email, username: Username): F[Unit] = authEmail.sendPasswordChangedNotification(email, username)
      }

}
