package com.sumup.unit.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.{IntField, RecordField}
import org.scalatest.{Outcome, fixture}

class RecordFieldSpec extends fixture.FunSpec {
  type FixtureParam = RecordField
  val name = "test-field"
  val field = IntField("test-field")
  val fields = List(field)

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = RecordField(name, fields)
    test(fixture)
  }

  describe("#apply") {
    describe("with no `isIdentity` argument") {
      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.RECORD`") { fixture =>
        assert(fixture.`type` == FieldType.RECORD)
      }

      it("has `fields`") { fixture =>
        assert(fixture.fields == fields)
      }
      it("has false `isIdentity") { fixture => assert(!fixture.isIdentity) }
    }

    describe("with `isIdentity` argument") {
      val isIdentity = true
      val fixture = RecordField(name, fields, isIdentity)

      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.RECORD`") { fixture =>
        assert(fixture.`type` == FieldType.RECORD)
      }

      it("has `fields`") { fixture =>
        assert(fixture.fields == fields)
      }

      it("has `isIdentity") { _ => assert(fixture.isIdentity == isIdentity) }
    }
  }
}
