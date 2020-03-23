package io.tictactoe.authentication.services

import cats.effect.Sync
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.values.{Email, Yes}
import cats.implicits._
import io.tictactoe.authentication.errors.IllegalConfirmationToken
import io.tictactoe.authentication.events.PasswordChangedEvent
import io.tictactoe.authentication.model.{PasswordChangeRequest, User}
import io.tictactoe.base.logging.Logging
import io.tictactoe.base.tokens.TokenGenerator
import io.tictactoe.base.utils.Syntax._
import io.tictactoe.base.uuid.UUIDGenerator
import io.tictactoe.calendar.Calendar
import io.tictactoe.events.bus.EventBus

trait PasswordChanger[F[_]] {

  def request(email: Email): F[Unit]

  def changePassword(request: PasswordChangeRequest): F[Unit]

}

object PasswordChanger {

  def apply[F[_]](implicit ev: PasswordChanger[F]): PasswordChanger[F] = ev

  def live[F[_]: Sync: AuthRepository: TokenGenerator: AuthEmail: Logging: PasswordHasher: EventBus: Calendar: UUIDGenerator]
    : F[PasswordChanger[F]] =
    for {
      logger <- Logging[F].create[PasswordChanger[F]]
    } yield
      new PasswordChanger[F] {

        override def request(email: Email): F[Unit] =
          for {
            user <- AuthRepository[F].getByEmail(email)
            _ <- user match {
              case Some(User(id, username, _, _, Yes, _, _)) =>
                for {
                  token <- TokenGenerator[F].generate
                  _ <- logger.info(show"Sending of password reset mail requested for user with id = $id and email = $email.")
                  _ <- AuthRepository[F].updatePasswordResetToken(id, token)
                  _ <- AuthEmail[F].sendPasswordChangeRequest(email, username, id, token)
                } yield ()
              case _ => logger.info(show"No confirmed user with mail $email found in database, sending no email.")
            }
          } yield ()

        override def changePassword(request: PasswordChangeRequest): F[Unit] = request match {
          case PasswordChangeRequest(userId, token, newPassword) =>
            for {
              _ <- AuthRepository[F].getById(userId).throwIfEmptyFilter(IllegalConfirmationToken)(_.passwordResetToken.contains(token))
              hash <- PasswordHasher[F].hash(newPassword)
              _ <- AuthRepository[F].updateHash(userId, hash)
              _ <- logger.info(show"Password of user with id $userId was changed.")
              _ <- EventBus[F].publishF(PasswordChangedEvent.create(userId))
            } yield ()
        }
      }

}
