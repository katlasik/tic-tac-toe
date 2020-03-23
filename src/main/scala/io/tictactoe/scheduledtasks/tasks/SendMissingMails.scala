package io.tictactoe.scheduledtasks.tasks

import io.tictactoe.emails.services.EmailSender
import io.tictactoe.scheduler.DurationSchedulerTask

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

final class SendMissingMails[F[_]: EmailSender] extends DurationSchedulerTask[F] {
  override val interval: FiniteDuration = 15.minutes

  override def executedTask: F[Unit] = EmailSender[F].sendMissingEmails()
}
