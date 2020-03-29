package io.tictactoe.authentication.templates

import io.tictactoe.infrastructure.templates.model.TemplateData

final case class RegistrationMailTemplateData(username: String, link: String) extends TemplateData {
  override val path: String = "registration"
}
