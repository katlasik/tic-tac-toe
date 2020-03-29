package io.tictactoe.authentication.templates

import io.tictactoe.infrastructure.templates.model.TemplateData

final case class PasswordChangedMailTemplateData(username: String) extends TemplateData {
  override val path: String = "password_changed"
}
