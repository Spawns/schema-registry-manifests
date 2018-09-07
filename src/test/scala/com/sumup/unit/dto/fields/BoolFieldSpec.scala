package com.sumup.unit.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.BoolField
import org.scalatest.{Outcome, fixture}

class BoolFieldSpec extends fixture.FunSpec {
  type FixtureParam = BoolField
  val name = "test-field"

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = BoolField(name)
    test(fixture)
  }

  describe("#apply") {
    describe("with no `isIdentity` argument") {
      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.BOOL`") { fixture =>
        assert(fixture.`type` == FieldType.BOOL)
      }
      it("has false `isIdentity") { fixture => assert(!fixture.isIdentity) }
    }

    describe("with `isIdentity` argument") {
      val isIdentity = true
      val fixture = BoolField(name, isIdentity)

      it("has `name`") { _ => assert(fixture.name == name) }
      it("has `type` that is `FieldType.BOOL`") { _ =>
        assert(fixture.`type` == FieldType.BOOL)
      }
      it("has `isIdentity") { _ => assert(fixture.isIdentity == isIdentity) }
    }
  }
}
