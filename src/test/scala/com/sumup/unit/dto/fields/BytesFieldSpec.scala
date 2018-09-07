package com.sumup.unit.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.BytesField
import org.scalatest.{Outcome, fixture}

class BytesFieldSpec extends fixture.FunSpec {
  type FixtureParam = BytesField
  val name = "test-field"

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = BytesField(name)
    test(fixture)
  }

  describe("#apply") {
    describe("with no `isIdentity` argument") {
      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.BYTES`") { fixture => assert(fixture.`type` == FieldType.BYTES) }
      it("has false `isIdentity") { fixture => assert(!fixture.isIdentity) }
    }

    describe("with `isIdentity` argument") {
      val isIdentity = true
      val fixture = BytesField(name, isIdentity)

      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.BYTES`") { fixture => assert(fixture.`type` == FieldType.BYTES) }
      it("has `isIdentity") { _ => assert(fixture.isIdentity == isIdentity) }
    }
  }
}
