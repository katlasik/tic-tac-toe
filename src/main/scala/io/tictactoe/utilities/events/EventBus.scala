package io.tictactoe.utilities.events

import java.util.concurrent.Executors

import cats.effect.{Concurrent, Sync}
import cats.implicits._
import fs2.concurrent.Queue
import fs2.Stream
import io.tictactoe.utilities.events.error.FullEventBusError
import io.tictactoe.utilities.events.model.Event
import io.tictactoe.utilities.logging.Logging

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object EventBus {

  private val QueueSize = 100

  val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  def apply[F[_]]()(implicit ev: EventBus[F]): EventBus[F] = ev

  def create[F[_]: Concurrent: Sync: Logging](): F[EventBus[F]] =
    for {
      q: Queue[F, Event] <- Queue.bounded[F, Event](QueueSize)
    } yield
      new EventBus[F] {
        override def publish(event: Event): F[Unit] = q.offer1(event).flatMap {
          case true  => Sync[F].unit
          case false => Sync[F].raiseError(FullEventBusError(event))
        }

        override def publishF(event: F[Event]): F[Unit] =
          for {
            e <- event
            _ <- publish(e)
          } yield ()

        override def subscribe: Stream[F, Event] = q.dequeue
      }

}

trait EventBus[F[_]] {
  def publish(event: Event): F[Unit]

  def publishF(event: F[Event]): F[Unit]

  def subscribe: Stream[F, Event]
}
