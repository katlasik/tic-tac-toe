package io.tictactoe.utilities.emails.services

import java.util.Properties

import cats.effect.Sync
import cats.implicits._
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.configuration.model.{MailServer, Smtp}
import io.tictactoe.utilities.emails.EmailTransport
import io.tictactoe.utilities.emails.model.EmailMessage
import io.tictactoe.utilities.emails.syntax._
import io.tictactoe.utilities.logging.Logging
import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, PasswordAuthentication, Session, Transport}

import scala.jdk.CollectionConverters._
import scala.util.chaining._

private[emails] object LiveEmailTransport {

  def create[F[_]: Sync: Configuration: Logging]: F[EmailTransport[F]] = {

    def prepareMessage(email: EmailMessage, session: Session): F[MimeMessage] =
      for {
        from <- email.sender.toAddress()
        to <- email.recipients.traverse(_.toAddress())
      } yield {
        val message = new MimeMessage(session)
        message.addFrom(Array(from))
        message.addRecipients(RecipientType.TO, to.toList.toArray)
        message.setText(email.text.value)
        message.setSubject(email.title.value)
        message
      }

    def createAuthenticator(username: String, password: String): F[Authenticator] = Sync[F].delay(
      new Authenticator {
        override def getPasswordAuthentication: PasswordAuthentication =
          new PasswordAuthentication(username, password)
      }
    )

    def createProperties(host: String, port: Int): Properties =
      new Properties(System.getProperties)
        .tap(
          _.putAll(
            Map(
              "mail.smtp.host" -> host,
              "mail.smtp.port" -> port.toString
            ).asJava
          )
        )

    def createSession(): F[Session] =
      for {
        MailServer(username, password, Smtp(host, port)) <- Configuration[F].access().map(_.mail.server)
        authenticator <- createAuthenticator(username, password)
      } yield Session.getInstance(createProperties(host, port), authenticator)

    for {
      session <- createSession()
      logger <- Logging[F].create[EmailTransport[F]]
    } yield
      new EmailTransport[F] {

        override def send(email: EmailMessage): F[Unit] =
          for {
            message <- prepareMessage(email, session)
            _ <- Sync[F].delay(Transport.send(message))
            _ <- logger.info(s"Email sent to ${email.recipients.mkString_(", ")}.")
          } yield ()

      }
  }

}
