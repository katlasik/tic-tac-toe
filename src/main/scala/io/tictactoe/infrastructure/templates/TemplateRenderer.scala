package io.tictactoe.infrastructure.templates

import cats.effect.Sync
import org.fusesource.scalate._
import cats.implicits._
import io.tictactoe.infrastructure.templates.errors.TemplateRenderingError
import io.tictactoe.infrastructure.templates.model.{RenderedTemplate, TemplateDataValues}

trait TemplateRenderer[F[_]] {

  def renderTemplateAndTitle(template: TemplateDataValues): F[RenderedTemplate]

}

object TemplateRenderer {

  def apply[F[_]]()(implicit ev: TemplateRenderer[F]): TemplateRenderer[F] = ev

  def live[F[_]: Sync]: F[TemplateRenderer[F]] = {

    def basePath(path: String): String = s"/templates/mails/$path.mustache"

    val SectionSeparator: String = "---"

    for {
      engine <- Sync[F].delay(new TemplateEngine)
    } yield
      new TemplateRenderer[F] {
        override def renderTemplateAndTitle(data: TemplateDataValues): F[RenderedTemplate] =
          for {
            rendered <- Sync[F].delay(engine.layout(basePath(data.templatePath), data.values))
            result <- rendered.split(SectionSeparator) match {
              case Array(title, template) => Sync[F].pure(RenderedTemplate(title.trim, template.trim))
              case _                      => Sync[F].raiseError(TemplateRenderingError("Can't find title section in template."))
            }
          } yield result
      }
  }

}
