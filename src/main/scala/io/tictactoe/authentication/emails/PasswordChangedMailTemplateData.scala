package io.tictactoe.authentication.emails

import io.tictactoe.infrastructure.templates.model.TemplateData
import io.tictactoe.values.Username

final case class PasswordChangedMailTemplateData(username: Username) extends TemplateData {
  override val path: String = "password_changed"
}
