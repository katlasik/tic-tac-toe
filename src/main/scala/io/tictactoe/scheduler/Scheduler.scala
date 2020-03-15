package io.tictactoe.scheduler

import cats.effect.{Concurrent, Sync, Timer}
import cats.implicits._
import cron4s.Cron
import eu.timepit.fs2cron.awakeEveryCron
import fs2.Stream
import io.tictactoe.base.logging.Logging

trait Scheduler[F[_]] {

  def schedule(tasks: List[ScheduledTask[F]]): F[Unit]

}

object Scheduler {

  def apply[F[_]](implicit ev: Scheduler[F]): Scheduler[F] = ev

  def live[F[_]: Sync: Timer: Logging: Concurrent]: F[Scheduler[F]] = {

    def awakingStream(task: ScheduledTask[F]): Stream[F, Unit] = task match {
      case c: CronScheduledTask[F] => Stream.fromEither[F](Cron.parse(c.cronExpression)).flatMap(cron => awakeEveryCron(cron))
      case d: DurationSchedulerTask[F] => Stream.awakeEvery[F](d.interval).as(())
    }

    for {
      logger <- Logging[F].create[Scheduler.type]
    } yield
      new Scheduler[F] {
        override def schedule(tasks: List[ScheduledTask[F]]): F[Unit] = {

          val result = tasks.traverse { t =>
            for {
              _ <- awakingStream(t) >> Stream.eval(t.executedTask)
            } yield ()
          }

          Concurrent[F].start(result.compile.drain).void *> logger.info(s"Starting ${tasks.size} scheduler task(s).")
        }
      }
  }

}
