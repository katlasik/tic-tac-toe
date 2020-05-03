package io.tictactoe.authentication.infrastructure.emails

import io.tictactoe.utilities.templates.model.TemplateData
import io.tictactoe.values.{Link, Username}

final case class PasswordResetMailTemplateData(username: Username, link: Link) extends TemplateData {
  override val path: String = "password_reset"
}
