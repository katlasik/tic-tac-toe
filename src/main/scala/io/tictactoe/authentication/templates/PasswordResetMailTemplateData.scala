package io.tictactoe.authentication.templates

import io.tictactoe.base.templates.model.TemplateData

final case class PasswordResetMailTemplateData(username: String, link: String) extends TemplateData {
  override val path: String = "password"
}
