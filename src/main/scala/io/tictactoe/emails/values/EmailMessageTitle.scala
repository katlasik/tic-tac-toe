package io.tictactoe.emails.values

final case class EmailMessageTitle(value: String) extends AnyVal

object EmailMessageTitle {

  implicit class EmailMessageTitleExtension(text: String) {
    def toEmailTitle: EmailMessageTitle = EmailMessageTitle(text)
  }

}
