package com.sumup.unit.json

import com.sumup.dto.FieldType.FieldType
import com.sumup.json.JsValidationHelpers
import org.scalatest.FunSpec
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, _}

class JsValidationHelpersSpec extends FunSpec {
  describe(".getPresentStringFieldOrThrow") {
    describe("""with " " (single-space string)""") {
      it("raises `deserializationError`") {
        val name = "foo"
        val field = Some(" ".toJson)
        val exception = intercept[DeserializationException] {
          JsValidationHelpers.getPresentStringFieldOrThrow(name, field)
        }

        assert(exception.getMessage == s"`$name` is blank/empty")
      }
    }

    describe("""with "" (blank string)""") {
      it("raises `deserializationError`") {
        val name = "foo"
        val field = Some("".toJson)
        val exception = intercept[DeserializationException] {
          JsValidationHelpers.getPresentStringFieldOrThrow(name, field)
        }

        assert(exception.getMessage == s"`$name` is blank/empty")
      }
    }

    describe("""with "foo" (present string)""") {
      it("""returns "foo"""") {
        val name = "foo"
        val value = "foo"
        val field = Some(value.toJson)
        assert(
          value == JsValidationHelpers.getPresentStringFieldOrThrow(name, field)
        )
      }
    }
  }

  describe(".getPositiveIntOrThrow") {
    describe("with invalid number") {
      it("raises `deserializationError`") {
        val name = "foo"
        val field = Some("-SCALA".toJson)
        val exception = intercept[DeserializationException] {
          JsValidationHelpers.getPositiveIntOrThrow(name, field)
        }

        assert(exception.getMessage == s"`$name` is not an int")
      }
    }

    describe("with negative int") {
      it("raises `deserializationError`") {
        val name = "foo"
        val value = -50
        val field = Some(value.toJson)
        val exception = intercept[DeserializationException] {
          JsValidationHelpers.getPositiveIntOrThrow(name, field)
        }

        assert(exception.getMessage == s"`$name` is not a valid positive int")
      }
    }

    describe("with positive int") {
      it("returns the positive int") {
        val name = "foo"
        val value = 11
        val field = Some(value.toJson)
        assert(value == JsValidationHelpers.getPositiveIntOrThrow(name, field))
      }
    }
  }

  describe(".getPresentAndKnownFieldTypeOrThrow") {
    describe("""with " " (single-space string)""") {
      it("raises `deserializationError`") {
        val name = "foo"
        val field = Some(" ".toJson)
        val exception = intercept[DeserializationException] {
          JsValidationHelpers.getPresentAndKnownFieldTypeOrThrow(name, field)
        }

        assert(exception.getMessage == s"`$name` is blank/empty")
      }
    }

    describe("""with "" (blank string)""") {
      it("raises `deserializationError`") {
        val name = "foo"
        val field = Some("".toJson)
        val exception = intercept[DeserializationException] {
          JsValidationHelpers.getPresentAndKnownFieldTypeOrThrow(name, field)
        }

        assert(exception.getMessage == s"`$name` is blank/empty")
      }
    }

    describe("with string that is not afield type") {
      it("""returns "foo"""") {
        val name = "foo"
        val value = "foo"
        val field = Some(value.toJson)
        val exception = intercept[DeserializationException] {
          JsValidationHelpers.getPresentAndKnownFieldTypeOrThrow(name, field)
        }

        assert(exception.getMessage == s"`$name` is not a known field type")
      }
    }

    describe("with string that is a field type") {
      it("returns a `FieldType`") {
        val fieldName = "foo"
        val value = "string"
        val field = Some(value.toJson)
        val returnValue = JsValidationHelpers.getPresentAndKnownFieldTypeOrThrow(fieldName, field)
        assert(returnValue.isInstanceOf[FieldType])
        assert(returnValue.toString == value)
      }
    }
  }
}
