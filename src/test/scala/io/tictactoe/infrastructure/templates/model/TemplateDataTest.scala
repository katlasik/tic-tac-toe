package io.tictactoe.infrastructure.templates.model

import cats.Show
import io.tictactoe.testutils.Fixture
import org.scalatest.{FlatSpec, Matchers}
import shapeless.test.illTyped

class TemplateDataTest extends FlatSpec with Matchers {

  final case class WithShow(value: String)
  final case class NoShow(value: String)
  implicit val show: Show[WithShow] = Show.show(_.value)

  it should "allow rendering TemplateData to map if all members have show instance in scope" in new Fixture {

    final case class Test(user: WithShow, token: WithShow) extends TemplateData {
      override val path: String = "path"
    }

    Test(WithShow("user"), WithShow("123")).values shouldBe Map("user" -> "user", "token" -> "123")

  }

  it should "disallow rendering TemplateData to map if NOT all members have show instance in scope" in new Fixture {

    final case class Test(user: WithShow, token: NoShow) extends TemplateData {
      override val path: String = "path"
    }

    illTyped { """Test(WithShow("user"), NoShow("123")).values""" }

  }
}
