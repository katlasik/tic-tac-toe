package io.tictactoe.authentication.services

import java.security.SecureRandom

import cats.effect.Sync
import io.tictactoe.authentication.model.ConfirmationToken
import cats.implicits._

import scala.jdk.StreamConverters._

trait ConfirmationTokenGenerator[F[_]] {
  def generate: F[ConfirmationToken]
}

object ConfirmationTokenGenerator {

  val TokenSize: Long = 28L
  val AllowedCharacters: Seq[Char] = ('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z')

  def apply[F[_]](implicit ev: ConfirmationTokenGenerator[F]): ConfirmationTokenGenerator[F] = ev

  def live[F[_]: Sync]: F[ConfirmationTokenGenerator[F]] =
    for {
      random <- Sync[F].delay(new SecureRandom)
    } yield
      new ConfirmationTokenGenerator[F] {
        override def generate: F[ConfirmationToken] =
          for {
            token <- Sync[F].delay(random.ints(TokenSize, 0, AllowedCharacters.size).toScala(List).map(AllowedCharacters(_)))
          } yield ConfirmationToken(token.mkString)
      }

}
