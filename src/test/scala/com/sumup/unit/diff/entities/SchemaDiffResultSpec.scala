package com.sumup.unit.diff.entities

import com.sumup.diff.entities.{Add, SchemaDiffResult}
import com.sumup.diff.enums.FieldChangeOperationType
import com.sumup.dto.FieldType
import org.scalatest.{Outcome, fixture}
import spray.json.{JsObject, JsString}

class SchemaDiffResultSpec extends fixture.FunSpec {
  type FixtureParam = SchemaDiffResult
  val isSameSchema = false
  val isMajorUpgradable = true
  val isMinorUpgradable = true
  val isMajorUpgrade = false
  val isMinorUpgrade = false
  val fieldChanges = List(
    new Add(
      FieldChangeOperationType.ADD,
      "/-" ,
      JsObject(
        "name" -> JsString("example-field"),
        "type" -> JsString(FieldType.INT.toString)
      )
    )
  )

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = SchemaDiffResult(
      isSameSchema,
      isMajorUpgradable,
      isMinorUpgradable,
      isMajorUpgrade,
      isMinorUpgrade,
      fieldChanges
    )
    test(fixture)
  }

  describe(".apply") {
    it("has `isSameSchema`") { fixture => assert(fixture.isSameSchema == isSameSchema) }
    it("has `isMajorUpgradable`") { fixture => assert(fixture.isMajorUpgradable == isMajorUpgradable) }
    it("has `isMinorUpgradable`") { fixture => assert(fixture.isMinorUpgradable == isMinorUpgradable) }
    it("has `isMajorUpgrade`") { fixture => assert(fixture.isMajorUpgrade == isMajorUpgrade) }
    it("has `isMinorUpgrade`") { fixture => assert(fixture.isMinorUpgrade == isMinorUpgrade) }
    it("has `fieldChanges`") { fixture => assert(fixture.fieldChanges == fieldChanges) }
  }
}
