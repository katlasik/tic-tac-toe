package io.tictactoe.emails.values

final case class EmailMessageText(value: String) extends AnyVal

object EmailMessageText {

  implicit class EmailMessageTextExtension(text: String) {
    def toEmailText: EmailMessageText = EmailMessageText(text)
  }

}
