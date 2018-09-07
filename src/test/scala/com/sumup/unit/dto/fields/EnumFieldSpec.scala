package com.sumup.unit.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.EnumField
import org.scalatest.{Outcome, fixture}

class EnumFieldSpec extends fixture.FunSpec {
  type FixtureParam = EnumField[String]
  val name = "test-field"
  val allowedValues = List("String", "String")
  val valueType = FieldType.withName("string")

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = EnumField(name, allowedValues, valueType)
    test(fixture)
  }

  describe("#apply") {

    describe("with `string` enum") {
      it("has `name`") { fixture => assert(fixture.name == name) }
      it("has `allowedValues` strings") { fixture => assert(fixture.allowedValues == allowedValues) }
      it("has `valueType` string") { fixture => assert(fixture.valueType == valueType) }
      it("has `type` that is `FieldType.ENUM`") { fixture =>
        assert(fixture.`type` == FieldType.ENUM)
      }
    }

    describe("with `isIdentity` enum") {
      val isIdentity = true
      val fixture = EnumField(name, allowedValues, valueType, isIdentity)

      it("has `isIdentity`") { _ => assert(fixture.isIdentity == isIdentity) }
    }

    describe("with `long` enum") {
      val allowedValues = List(1L, 2L)
      val valueType = FieldType.withName("long")

      val fixture = EnumField(name, allowedValues, valueType)

      it("has `allowedValues` longs") { _ => assert(fixture.allowedValues == allowedValues) }
      it("has `valueType` long") { _ => assert(fixture.valueType == valueType) }
    }

    describe("with `double` enum") {
      val allowedValues = List(10.12341234, 12.12341234)
      val valueType = FieldType.withName("double")

      val fixture = EnumField(name, allowedValues, valueType)

      it("has `allowedValues` doubles") { _ => assert(fixture.allowedValues == allowedValues) }
      it("has `valueType` double") { _ => assert(fixture.valueType == valueType) }
    }

    describe("with `float` enum") {
      val allowedValues = List(10.1234, 12.1234)
      val valueType = FieldType.withName("float")

      val fixture = EnumField(name, allowedValues, valueType)

      it("has `allowedValues` floats") { _ => assert(fixture.allowedValues == allowedValues) }
      it("has `valueType` float") { _ => assert(fixture.valueType == valueType) }
    }
  }
}
