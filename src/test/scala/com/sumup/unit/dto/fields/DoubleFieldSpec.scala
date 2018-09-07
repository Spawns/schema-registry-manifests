package com.sumup.unit.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.DoubleField
import org.scalatest.{Outcome, fixture}

class DoubleFieldSpec extends fixture.FunSpec {
  type FixtureParam = DoubleField
  val name = "test-field"

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = DoubleField(name)
    test(fixture)
  }

  describe("#apply") {
    describe("with no `isIdentity` argument") {
      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.DOUBLE`") { fixture => assert(fixture.`type` == FieldType.DOUBLE) }
      it("has false `isIdentity") { fixture => assert(!fixture.isIdentity) }
    }

    describe("with `isIdentity` argument") {
      val isIdentity = true
      val fixture = DoubleField(name, isIdentity)

      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.DOUBLE`") { fixture => assert(fixture.`type` == FieldType.DOUBLE) }
      it("has `isIdentity") { _ => assert(fixture.isIdentity == isIdentity) }
    }
  }
}
