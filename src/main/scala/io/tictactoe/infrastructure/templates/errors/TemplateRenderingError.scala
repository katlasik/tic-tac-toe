package io.tictactoe.infrastructure.templates.errors

import io.tictactoe.error.BaseError

final case class TemplateRenderingError(override val msg: String) extends BaseError(msg)
