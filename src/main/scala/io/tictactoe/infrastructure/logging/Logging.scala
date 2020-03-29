package io.tictactoe.infrastructure.logging

import cats.effect.Sync
import org.slf4j.LoggerFactory
import cats.implicits._
import shapeless.the

import scala.reflect.ClassTag

trait Logging[F[_]] {
  def create[C: ClassTag]: F[Logger[F]]
}

trait Logger[F[_]] {
  def info(msg: => String): F[Unit]
  def error(msg: => String, throwable: Throwable): F[Unit]
}

object Logging {
  def live[F[_]: Sync]: Logging[F] = new Logging[F] {
    override def create[C: ClassTag]: F[Logger[F]] =
      for {
        logger <- Sync[F].delay(LoggerFactory.getLogger(the[ClassTag[C]].runtimeClass))
      } yield
        new Logger[F] {
          override def info(msg: => String): F[Unit] = Sync[F].delay(logger.info(msg))

          override def error(msg: => String, throwable: Throwable): F[Unit] = Sync[F].delay(logger.error(msg, throwable))
        }
  }

  def apply[F[_]]()(implicit ev: Logging[F]): Logging[F] = ev

}
