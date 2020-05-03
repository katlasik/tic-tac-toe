package io.tictactoe.utilities.templates.errors

import io.tictactoe.errors.BaseError

final case class TemplateRenderingError(override val msg: String) extends BaseError(msg)
