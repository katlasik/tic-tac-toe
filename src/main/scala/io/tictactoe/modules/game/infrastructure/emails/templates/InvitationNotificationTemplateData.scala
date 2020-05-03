package io.tictactoe.modules.game.infrastructure.emails.templates

import io.tictactoe.utilities.templates.model.TemplateData
import io.tictactoe.values.{Link, Username}

final case class InvitationNotificationTemplateData(host: Username, guest: Username, link: Link) extends TemplateData {
  override val path: String = "invitation_notification"
}
