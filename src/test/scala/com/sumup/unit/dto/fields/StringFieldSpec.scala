package com.sumup.unit.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.StringField
import org.scalatest.{Outcome, fixture}

class StringFieldSpec extends fixture.FunSpec {
  type FixtureParam = StringField
  val name = "test-field"

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = StringField(name)
    test(fixture)
  }

  describe("#apply") {
    describe("with no `isIdentity` argument") {
      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.STRING`") { fixture =>
        assert(fixture.`type` == FieldType.STRING)
      }
      it("has false `isIdentity") { fixture => assert(!fixture.isIdentity) }
    }

    describe("with `isIdentity` argument") {
      val isIdentity = true
      val fixture = StringField(name, isIdentity)

      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.STRING`") { fixture =>
        assert(fixture.`type` == FieldType.STRING)
      }
      it("has `isIdentity") { _ => assert(fixture.isIdentity == isIdentity) }
    }
  }
}
