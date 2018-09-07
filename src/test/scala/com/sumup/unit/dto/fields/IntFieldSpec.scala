package com.sumup.unit.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.IntField
import org.scalatest.{Outcome, fixture}

class IntFieldSpec extends fixture.FunSpec {
  type FixtureParam = IntField
  val name = "test-field"

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = IntField(name)
    test(fixture)
  }

  describe("#apply") {
    describe("with no `isIdentity` argument") {
      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.INT`") { fixture => assert(fixture.`type` == FieldType.INT) }
      it("has false `isIdentity") { fixture => assert(!fixture.isIdentity) }
    }

    describe("with `isIdentity` argument") {
      val isIdentity = true
      val fixture = IntField(name, isIdentity)

      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.INT`") { fixture => assert(fixture.`type` == FieldType.INT) }
      it("has `isIdentity") { _ => assert(fixture.isIdentity == isIdentity) }
    }
  }
}
