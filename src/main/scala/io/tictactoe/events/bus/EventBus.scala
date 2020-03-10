package io.tictactoe.events.bus

import java.util.concurrent.Executors

import cats.effect.{Concurrent, Sync}
import fs2.concurrent.Queue
import io.tictactoe.events.error.FullEventBusError
import cats.implicits._
import io.tictactoe.base.logging.Logging

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object EventBus {

  private val QueueSize = 100

  val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  def apply[F[_]]()(implicit ev: EventBus[F]): EventBus[F] = ev

  def start[F[_]: Concurrent: Sync: Logging](eventHandler: EventHandler[F]): F[EventBus[F]] =
    for {
      logger <- Logging[F].create[EventBus[F]]
      q: Queue[F, Event] <- Queue.bounded[F, Event](QueueSize)
      _ <- Concurrent[F]
        .start(
          q.dequeue
            .evalTap(
              e =>
                eventHandler
                  .handle(e)
                  .recoverWith(
                    logger.error("Unhandled error in event bus!", _)
                )
            )
            .compile
            .drain
        )
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
      }

}

trait EventBus[F[_]] {
  def publish(event: Event): F[Unit]

  def publishF(event: F[Event]): F[Unit]
}
