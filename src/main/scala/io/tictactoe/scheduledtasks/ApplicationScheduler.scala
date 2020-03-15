package io.tictactoe.scheduledtasks

import io.tictactoe.authentication.services.RegistrationEmail
import io.tictactoe.scheduledtasks.tasks.SendMissingRegistrationMails
import io.tictactoe.scheduler.{ScheduledTask, Scheduler}

trait ApplicationScheduler[F[_]] {

  def start(): F[Unit]

}

object ApplicationScheduler {

  def tasks[F[_]: Scheduler: RegistrationEmail]: List[ScheduledTask[F]] = List(
    new SendMissingRegistrationMails[F]
  )

  def live[F[_]: Scheduler: RegistrationEmail]: ApplicationScheduler[F] =
    () => Scheduler[F].schedule(tasks)

}
