package io.tictactoe.infrastructure.templates.model

import cats.Show
import io.tictactoe.testutils.Fixture
import org.scalatest.{FlatSpec, Matchers}
import shapeless.test.illTyped

class TemplateDataTest extends FlatSpec with Matchers  {

  case class WithShow(value: String)
  case class NoShow(value: String)
  implicit val show: Show[WithShow] = Show.show(_.value)

  it should "allow rendering TemplateData to map if all members have show instance in scope" in new Fixture {

    case class Test(user: WithShow, token: WithShow) extends TemplateData {
      override val path: String = "path"
    }

    val data = Test(WithShow("user"), WithShow("123"))

    data.values shouldBe Map("user" -> "user", "token" -> "123")
    data.templatePath shouldBe "path"

  }

  it should "disallow rendering TemplateData to map if NOT all members have show instance in scope" in new Fixture {

    case class TestIllegal(user: WithShow, token: NoShow) extends TemplateData {
      override val path: String = "path"
    }

    illTyped { """TestIllegal(WithShow("user"), NoShow("123")).values""" }

  }
}
