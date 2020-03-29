package io.tictactoe.infrastructure.scheduler

import scala.concurrent.duration.FiniteDuration

trait ScheduledTask[F[_]] {
  def executedTask: F[Unit]
}

trait CronScheduledTask[F[_]] extends ScheduledTask[F] {
  val cronExpression: String
}

trait DurationSchedulerTask[F[_]] extends ScheduledTask[F] {
  val interval: FiniteDuration
}
