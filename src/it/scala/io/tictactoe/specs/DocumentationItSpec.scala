package io.tictactoe.specs

import java.time.Instant

import better.files._
import io.tictactoe.testutils.ItTest
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen, Matchers}

class DocumentationItSpec extends FeatureSpec with GivenWhenThen with Matchers with ItTest with BeforeAndAfter {

  feature("Documentation generator") {

    scenario("Documentation is generated") {

      Given("the the server is started")

      Then("the documentation should be just generated")

      val fileName = config.docs.file

      fileName.toFile.attributes.creationTime().toInstant.isAfter(Instant.now().minusSeconds(10)) shouldBe true

    }
  }

}
