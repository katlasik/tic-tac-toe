package io.tictactoe.scheduler

import cats.effect.{Resource, Sync}
import io.tictactoe.scheduler.tasks.SendMissingMails
import io.tictactoe.utilities.emails.EmailSender
import io.tictactoe.utilities.scheduler.{ScheduledTask, Scheduler}

trait ApplicationScheduler[F[_]] {

  def start(): Resource[F, Unit]

}

object ApplicationScheduler {

  def tasks[F[_]: Scheduler: EmailSender: Sync]: List[ScheduledTask[F]] = List(
    new SendMissingMails[F]
  )

  def live[F[_]: Scheduler: EmailSender: Sync]: ApplicationScheduler[F] = new ApplicationScheduler[F] {
    override def start(): Resource[F, Unit] = Scheduler[F].schedule(tasks)
  }

}
