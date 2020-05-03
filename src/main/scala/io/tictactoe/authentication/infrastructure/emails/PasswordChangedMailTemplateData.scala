package io.tictactoe.authentication.infrastructure.emails

import io.tictactoe.utilities.templates.model.TemplateData
import io.tictactoe.values.Username

final case class PasswordChangedMailTemplateData(username: Username) extends TemplateData {
  override val path: String = "password_changed"
}
