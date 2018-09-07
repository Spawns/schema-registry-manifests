package com.sumup.unit.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.FloatField
import org.scalatest.{Outcome, fixture}

class FloatFieldSpec extends fixture.FunSpec {
  type FixtureParam = FloatField
  val name = "test-field"

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = FloatField(name)
    test(fixture)
  }

  describe("#apply") {
    describe("with no `isIdentity` argument") {
      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.FLOAT`") { fixture => assert(fixture.`type` == FieldType.FLOAT) }
      it("has false `isIdentity") { fixture => assert(!fixture.isIdentity) }
    }

    describe("with `isIdentity` argument") {
      val isIdentity = true
      val fixture = FloatField(name, isIdentity)

      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.FLOAT`") { fixture => assert(fixture.`type` == FieldType.FLOAT) }
      it("has `isIdentity") { _ => assert(fixture.isIdentity == isIdentity) }
    }
  }
}
