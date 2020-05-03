package io.tictactoe.specs

import io.tictactoe.testutils.ItTest
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen, Matchers}

class DocumentationItSpec extends FeatureSpec with GivenWhenThen with Matchers with ItTest with BeforeAndAfter {

  feature("Documentation generator") {

    scenario("Documentation is generated") {

      Given("the the server is started")

      Then("the documentation should be just generated")

      get(baseUrl("docs")).success.plain should not be empty

    }
  }

}
