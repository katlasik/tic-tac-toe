package io.tictactoe.game.infrastructure.emails.templates

import io.tictactoe.infrastructure.templates.model.TemplateData
import io.tictactoe.values.{Link, Username}

final case class InvitationTemplateData(host: Username, link: Link) extends TemplateData {
  override val path: String = "invitation"
}
