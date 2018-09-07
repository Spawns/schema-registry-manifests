package com.sumup.unit.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.LongField
import org.scalatest.{Outcome, fixture}

class LongFieldSpec extends fixture.FunSpec {
  type FixtureParam = LongField
  val name = "test-field"

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = LongField(name)
    test(fixture)
  }

  describe("#apply") {
    describe("with no `isIdentity` argument") {
      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.LONG`") { fixture => assert(fixture.`type` == FieldType.LONG) }
      it("has false `isIdentity") { fixture => assert(!fixture.isIdentity) }
    }

    describe("with `isIdentity` argument") {
      val isIdentity = true
      val fixture = LongField(name, isIdentity)

      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.LONG`") { fixture => assert(fixture.`type` == FieldType.LONG) }
      it("has `isIdentity") { _ => assert(fixture.isIdentity == isIdentity) }
    }
  }
}
