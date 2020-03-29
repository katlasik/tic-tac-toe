package io.tictactoe.infrastructure.tokens

import java.security.SecureRandom

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.infrastructure.tokens.values.ConfirmationToken

import scala.jdk.StreamConverters._

trait TokenGenerator[F[_]] {
  def generate: F[ConfirmationToken]
}

object TokenGenerator {

  val TokenSize: Long = 28L
  val AllowedCharacters: Seq[Char] = ('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z')

  def apply[F[_]](implicit ev: TokenGenerator[F]): TokenGenerator[F] = ev

  def live[F[_]: Sync]: F[TokenGenerator[F]] =
    for {
      random <- Sync[F].delay(new SecureRandom)
    } yield
      new TokenGenerator[F] {
        override def generate: F[ConfirmationToken] =
          for {
            token <- Sync[F].delay(random.ints(TokenSize, 0, AllowedCharacters.size).toScala(List).map(AllowedCharacters(_)))
          } yield ConfirmationToken(token.mkString)
      }

}
