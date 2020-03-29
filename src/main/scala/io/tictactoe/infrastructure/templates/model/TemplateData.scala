package io.tictactoe.infrastructure.templates.model

trait TemplateData { self: Product =>
  val path: String

  def values: Map[String, Any] = productElementNames.zip(productIterator).toMap

}
