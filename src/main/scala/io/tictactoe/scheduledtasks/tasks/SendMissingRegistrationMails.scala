package io.tictactoe.scheduledtasks.tasks

import io.tictactoe.authentication.services.RegistrationEmail
import io.tictactoe.scheduler.DurationSchedulerTask

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

final class SendMissingRegistrationMails[F[_]: RegistrationEmail] extends DurationSchedulerTask[F]{
  override val interval: FiniteDuration = 15.minutes

  override def executedTask: F[Unit] = RegistrationEmail[F].resendMissingEmails()
}
