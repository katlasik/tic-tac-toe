package io.tictactoe.testutils

import io.circe.Json
import io.circe.parser.parse
import scala.util.chaining._

import scala.util.Try

case class CapturedMail(extractedText: String, recipient: String)

trait Mails { _: ItTest with Containers =>

  def mailSmtpPort: Int = Try(sys.env("IT_TEST_MAIL_SMTP_PORT").toInt).getOrElse(mailContainer.mappedPort(1025))

  def mailRestPort: Int = Try(sys.env("IT_TEST_MAIL_REST_PORT").toInt).getOrElse(mailContainer.mappedPort(8025))

  def getFirstMailContaining(expected: String): CapturedMail = getFirstMailMatching(s => Option.when(s.contains(expected))(s))

  def getFirstMailMatching(extractor: String => Option[String]): CapturedMail =
    repeatUntil(failureMsg = "Confirmation mail message not received in required time.") {
      val response = get(s"http://localhost:$mailRestPort/api/v2/messages").success.plain

      for {
        values <- parse(response).getOrElse(Json.Null).hcursor.downField("items").values
        value <- values.headOption
        (body, mailRecipient) <- value
          .hcursor
          .downField("Content")
          .pipe(
            cursor =>
              cursor
                .downField("Body")
                .as[String]
                .toOption
                .lazyZip(
                  cursor.downField("Headers").downField("To").as[List[String]].toOption.flatMap(_.headOption)
                )
          ).headOption
        extractedFromBody <- extractor(body)
      } yield CapturedMail(extractedFromBody, mailRecipient)

    }

}
