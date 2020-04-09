package io.tictactoe.authentication.emails

import io.tictactoe.infrastructure.templates.model.TemplateData
import io.tictactoe.values.{Link, Username}

final case class RegistrationMailTemplateData(username: Username, link: Link) extends TemplateData {
  override val path: String = "registration"
}
