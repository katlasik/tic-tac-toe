package io.tictactoe.scheduler

import cats.effect.Sync
import io.tictactoe.emails.services.EmailSender
import io.tictactoe.scheduler.tasks.SendMissingMails
import io.tictactoe.infrastructure.scheduler.{ScheduledTask, Scheduler}

trait ApplicationScheduler[F[_]] {

  def start(): F[Unit]

}

object ApplicationScheduler {

  def tasks[F[_]: Scheduler: EmailSender: Sync]: List[ScheduledTask[F]] = List(
    new SendMissingMails[F]
  )

  def live[F[_]: Scheduler: EmailSender: Sync]: ApplicationScheduler[F] =
    () => Scheduler[F].schedule(tasks)

}