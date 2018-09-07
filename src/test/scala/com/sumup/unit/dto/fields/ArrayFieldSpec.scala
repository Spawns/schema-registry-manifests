package com.sumup.unit.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.ArrayField
import org.scalatest.{Outcome, fixture}

class ArrayFieldSpec extends fixture.FunSpec {
  type FixtureParam = ArrayField
  val name = "test-field"
  val items = FieldType.INT

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = ArrayField(name, items)
    test(fixture)
  }

  describe("#apply") {
    describe("with no `isIdentity` argument") {
      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `type` that is `FieldType.ARRAY`") { fixture =>
        assert(fixture.`type` == FieldType.ARRAY)
      }
      it("has `items`") { fixture => assert(fixture.items == items) }
      it("has false `isIdentity") { fixture => assert(!fixture.isIdentity)}
    }

    describe("with `isIdentity` argument") {
      val isIdentity = true
      val fixture = ArrayField(name, items, isIdentity)

      it("has `name`") { _ => assert(fixture.name == name) }
      it("has `type` that is `FieldType.ARRAY`") { _ =>
        assert(fixture.`type` == FieldType.ARRAY)
      }
      it("has `items`") { _ => assert(fixture.items == items) }
      it("has `isIdentity") { _ => assert(fixture.isIdentity == isIdentity)}
    }
  }
}
