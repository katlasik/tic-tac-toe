package io.tictactoe.utilities.emails.services

import cats.data.NonEmptyList
import cats.effect.Sync
import io.tictactoe.utilities.uuid.UUIDGenerator
import io.tictactoe.utilities.database.Database
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.{Get, Put}
import io.tictactoe.utilities.emails.model.{EmailMessage, MissingEmail}
import io.tictactoe.utilities.emails.values.MailId
import io.tictactoe.values.Email
import io.tictactoe.utilities.emails.EmailRepository

private[emails] object LiveEmailRepository {

  implicit val put: Put[NonEmptyList[Email]] = Put[Array[String]].contramap(_.toList.map(_.value).toArray)
  implicit val get: Get[NonEmptyList[Email]] = Get[String].map {
    case s"{$emails}" => NonEmptyList.fromListUnsafe(emails.split(",").map(e => Email(e.trim)).toList)
  }

  def postgresql[F[_]: Database: UUIDGenerator: Sync]: EmailRepository[F] = new EmailRepository[F] {
    val transactor: HikariTransactor[F] = Database[F].transactor()

    override def save(email: EmailMessage): F[MailId] = email match {
      case EmailMessage(recipients, sender, text, title) =>
        for {
          id <- MailId.next[F]
          _ <- sql"INSERT INTO emails(id, recipients, sender, text, title) VALUES ($id, $recipients, $sender,$text, $title) ".update.run
            .transact(transactor)
        } yield id
    }

    override def confirm(mailId: MailId): F[Unit] =
      sql"UPDATE emails SET sent_on = NOW() WHERE id = $mailId".update.run.transact(transactor).void

    override def missingEmails(): F[List[MissingEmail]] =
      sql"SELECT id, recipients, sender, text, title FROM emails WHERE sent_on IS NULL".query[MissingEmail].to[List].transact(transactor)
  }

}
