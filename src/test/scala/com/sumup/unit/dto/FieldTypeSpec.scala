package com.sumup.unit.dto

import com.sumup.dto.FieldType
import org.scalatest.FunSpec

class FieldTypeSpec extends FunSpec {
  describe(".NULL") {
    it("has string value `null`") {
      assert(FieldType.NULL.toString == "null")
    }
  }

  describe(".BOOL") {
    it("has string value `boolean`") {
      assert(FieldType.BOOL.toString == "boolean")
    }
  }

  describe(".INT") {
    it("has string value `int`") {
      assert(FieldType.INT.toString == "int")
    }
  }

  describe(".LONG") {
    it("has string value `long`") {
      assert(FieldType.LONG.toString == "long")
    }
  }

  describe(".FLOAT") {
    it("has string value `float`") {
      assert(FieldType.FLOAT.toString == "float")
    }
  }

  describe(".DOUBLE") {
    it("has string value `double`") {
      assert(FieldType.DOUBLE.toString == "double")
    }
  }

  describe(".BYTES") {
    it("has string value `bytes`") {
      assert(FieldType.BYTES.toString == "bytes")
    }
  }

  describe(".STRING") {
    it("has string value `string`") {
      assert(FieldType.STRING.toString == "string")
    }
  }

  describe(".isType") {
    describe("with `null`") {
      it("is `true`") { assert(FieldType.isType("null")) }
    }

    describe("with `boolean`") {
      it("is `true`") { assert(FieldType.isType("boolean")) }
    }

    describe("with `int`") {
      it("is `true`") { assert(FieldType.isType("int")) }
    }

    describe("with `long`") {
      it("is `true`") { assert(FieldType.isType("long")) }
    }

    describe("with `float`") {
      it("is `true`") { assert(FieldType.isType("float")) }
    }

    describe("with `double`") {
      it("is `true`") { assert(FieldType.isType("double")) }
    }

    describe("with `bytes`") {
      it("is `true`") { assert(FieldType.isType("bytes")) }
    }

    describe("with `string`") {
      it("is `true`") { assert(FieldType.isType("string")) }
    }

    describe("with `test") {
      it("is `false`") { assert(!FieldType.isType("test")) }
    }
  }

  describe(".isEnumFieldType") {
    describe("with `string`") {
      it("is `true`") { assert(FieldType.isEnumFieldType("string")) }
    }

    describe("with `int`") {
      it("is `true`") { assert(FieldType.isEnumFieldType("int")) }
    }

    describe("with `long`") {
      it("is `true`") { assert(FieldType.isEnumFieldType("long")) }
    }

    describe("with `double`") {
      it("is `true`") { assert(FieldType.isEnumFieldType("double")) }
    }

    describe("with `float`") {
      it("is `true`") { assert(FieldType.isEnumFieldType("float")) }
    }

    describe("with `boolean`") {
      it("is `false`") { assert(!FieldType.isEnumFieldType("boolean")) }
    }

    describe("with `bytes`") {
      it("is `false`") { assert(!FieldType.isEnumFieldType("bytes")) }
    }

    describe("with `null") {
      it("is `false`") { assert(!FieldType.isEnumFieldType("null")) }
    }

    describe("with `test") {
      it("is `false`") { assert(!FieldType.isEnumFieldType("test")) }
    }
  }
}
