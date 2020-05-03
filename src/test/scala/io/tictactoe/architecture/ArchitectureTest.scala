package io.tictactoe.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import io.tictactoe.testutils.Fixture
import org.scalatest.FlatSpec
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes


class ArchitectureTest  extends FlatSpec {

  val imported = new ClassFileImporter().importPackages("io.tictactoe")

  "The logic in infrastructure package" should "not depend on the logic in domain package" in new Fixture {

    val rule = classes().that().resideInAPackage("..infrastructure..")
      .should().onlyAccessClassesThat().resideOutsideOfPackage("..domain..")

    rule.check(imported)

  }
}
