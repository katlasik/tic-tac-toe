package io.tictactoe.base.templates

import cats.effect.Sync
import org.fusesource.scalate._
import cats.implicits._
import io.tictactoe.base.templates.errors.TemplateRenderingError
import io.tictactoe.base.templates.model.{RenderedTemplate, TemplateData}

trait TemplateRenderer[F[_]] {

  def renderTemplateAndTitle(template: TemplateData): F[RenderedTemplate]

}

object TemplateRenderer {

  def apply[F[_]]()(implicit ev: TemplateRenderer[F]): TemplateRenderer[F] = ev

  def live[F[_]: Sync]: F[TemplateRenderer[F]] = {

    def basePath(path: String): String = s"/templates/mails/$path.mustache"

    val Separator = "---"

    for {
      engine <- Sync[F].delay(new TemplateEngine)
    } yield
      new TemplateRenderer[F] {
        override def renderTemplateAndTitle(template: TemplateData): F[RenderedTemplate] =
          for {
            rendered <- Sync[F].delay(engine.layout(basePath(template.path), template.values))
            result <- rendered.split(Separator) match {
              case Array(title, template) => Sync[F].pure(RenderedTemplate(title.trim, template.trim))
              case _                      => Sync[F].raiseError(TemplateRenderingError("Can't find title section in template."))
            }
          } yield result
      }
  }

}
