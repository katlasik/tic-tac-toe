package io.tictactoe.modules.authentication.infrastructure.emails

import io.tictactoe.utilities.templates.model.TemplateData
import io.tictactoe.values.{Link, Username}

final case class RegistrationMailTemplateData(username: Username, link: Link) extends TemplateData {
  override val path: String = "registration"
}
