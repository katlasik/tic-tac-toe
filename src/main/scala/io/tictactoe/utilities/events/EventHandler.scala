package io.tictactoe.utilities.events

import cats.effect.{Concurrent, Fiber, Resource}
import io.tictactoe.utilities.events.model.Event
import io.tictactoe.utilities.logging.{Logger, Logging}
import cats.implicits._

trait EventHandler[F[_]] {
  def handle(event: Event): F[Unit]

  def start(implicit L: Logging[F], C: Concurrent[F], eventBus: EventBus[F]): Resource[F, Unit] = {

    def startConsumer(logger: Logger[F]): F[Fiber[F, Unit]] =
      Concurrent[F]
        .start(
          eventBus.subscribe
            .evalTap(handle(_).recoverWith(logger.error("Unhandled error in event bus!", _)))
            .compile
            .drain
        )

    for {
      logger <- Resource.liftF(Logging[F].create[EventHandler[F]])
      handler <- Resource.make(startConsumer(logger))(_.cancel).void
    } yield handler
  }

}
