package com.sumup.unit.json

import com.sumup.dto.FieldType.FieldType
import com.sumup.dto.fields._
import com.sumup.dto.requests.{ConsumerRequest, SchemaRequest}
import com.sumup.dto.{Consumer, FieldType, Schema, ShortObjectId}
import com.sumup.json.SchemaRegistryJsonProtocol
import com.sumup.testutils.ObjectMother
import org.bson.types.ObjectId
import org.scalatest.FunSpec

class SchemaRegistryJsonProtocolSpec extends FunSpec {

  import com.sumup.json.SchemaRegistryJsonProtocol._
  import spray.json._

  describe(".fieldTypeFormat") {
    describe(".write") {
      it("writes the enum value as a string") {
        val value = FieldType.INT
        val expectedJsValue = JsString(value.toString)

        assert(expectedJsValue == SchemaRegistryJsonProtocol.fieldTypeFormat.write(value))
      }
    }

    describe(".read") {
      describe("with a `JsString`") {
        describe("that is a known `FieldType`") {
          it("returns a `FieldType`") {
            val value = FieldType.INT
            val jsValue = JsString(FieldType.INT.toString)

            val readFieldType = SchemaRegistryJsonProtocol.fieldTypeFormat.read(jsValue)

            assert(readFieldType.isInstanceOf[FieldType])
            assert(readFieldType == value)
          }
        }

        describe("that is not a known `FieldType`") {
          it("raises a `NoSuchElementException`") {
            val value = "foo"
            val jsValue = JsString(value)

            val exception = intercept[NoSuchElementException] {
              SchemaRegistryJsonProtocol.fieldTypeFormat.read(jsValue)
            }

            assert(exception.getMessage == s"No value found for '$value'")
          }
        }
      }

      describe("with other than a `JsString`") {
        it("raises a `DeserializationException`") {
          val value = 1
          val jsValue = JsNumber(value)

          val exception = intercept[DeserializationException] {
            SchemaRegistryJsonProtocol.fieldTypeFormat.read(jsValue)
          }

          assert(exception.getMessage == s"Expected a value from enum FieldType instead of $value")
        }
      }
    }
  }

  describe(".ShortObjectIdFormat") {
    describe(".read") {
      it("is not supported") {
        val exception = intercept[DeserializationException] {
          val objectId = new ObjectId()
          ShortObjectIdFormat.read(ShortObjectId.fromObjectId(objectId).toJson)
        }

        assert(exception.getMessage == "Not supported")
      }
    }

    describe(".write") {
      it("returns JsObject") {
        val objectId = new ObjectId()
        val jsObject = ShortObjectIdFormat.write(ShortObjectId.fromObjectId(objectId))

        val expectedJsObject = JsObject(
          "$id" -> JsNumber(objectId.getCounter),
          "timestamp" -> JsString(objectId.getDate.toInstant.toString)
        )
        assert(jsObject == expectedJsObject)
      }
    }
  }

  describe(".SchemaRequestFormat") {
    describe(".read") {
      describe("with present `name` field") {
        describe("with present `applicationId`") {
          describe("with present `majorVersion` that is a positive int field") {
            describe("with present `minorVersion` that is a positive int field") {
              describe("and `fields`") {
                it("returns a `SchemaRequest`") {
                  val name = "some-name"
                  val applicationId = "some-app"
                  val majorVersion = 1
                  val minorVersion = 2
                  val fieldName = "test-field"
                  val fieldType = FieldType.INT
                  val fieldIsIdentity = true

                  val value = JsObject(
                    "name" -> JsString(name),
                    "applicationId" -> JsString(applicationId),
                    "majorVersion" -> JsNumber(majorVersion),
                    "minorVersion" -> JsNumber(minorVersion),
                    "fields" -> JsArray(
                      JsObject(
                        "name" -> JsString(fieldName),
                        "type" -> JsString(fieldType.toString),
                        "isIdentity" -> JsBoolean(fieldIsIdentity)
                      )
                    )
                  )

                  val result = SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
                  assert(result.isInstanceOf[SchemaRequest])
                  assert(result.name == name)
                  assert(result.majorVersion == majorVersion)
                  assert(result.minorVersion == minorVersion)
                  assert(result.fields.length == 1)
                  val field = result.fields.head
                  assert(field.name == fieldName)
                  assert(field.`type` == fieldType)
                  assert(field.isIdentity == fieldIsIdentity)
                }
              }

              describe("and empty `fields` field") {
                it("raises `DeserializationError`") {
                  val value = JsObject(
                    "name" -> JsString("some-name"),
                    "applicationId" -> JsString("some-app"),
                    "majorVersion" -> JsNumber(1),
                    "minorVersion" -> JsNumber(2),
                    "fields" -> JsArray()
                  )

                  val exception = intercept[DeserializationException] {
                    SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
                  }

                  assert(exception.getMessage == "`fields` is empty")
                }
              }

              describe("and no `fields` field") {
                it("raises `DeserializationError`") {
                  val value = JsObject(
                    "name" -> JsString("some-name"),
                    "applicationId" -> JsString("some-app"),
                    "majorVersion" -> JsNumber(1),
                    "minorVersion" -> JsNumber(2)
                  )

                  val exception = intercept[DeserializationException] {
                    SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
                  }

                  assert(exception.getMessage == "`fields` is empty")
                }
              }
            }

            describe("with present `minorVersion` that is not a positive int field") {
              it("raises a `DeserializationError`") {
                val value = JsObject(
                  "name" -> JsString("some-name"),
                  "applicationId" -> JsString("some-app"),
                  "majorVersion" -> JsNumber(1),
                  "minorVersion" -> JsNumber(-1)
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
                }

                assert(exception.getMessage == "`minorVersion` is not a valid positive int")
              }
            }

            describe("with present `minorVersion` that is not an int field") {
              it("raises a `DeserializationError`") {
                val value = JsObject(
                  "name" -> JsString("some-name"),
                  "applicationId" -> JsString("some-app"),
                  "majorVersion" -> JsNumber(1),
                  "minorVersion" -> JsString("example")
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
                }

                assert(exception.getMessage == "`minorVersion` is not an int")
              }
            }

            describe("with blank `minorVersion` field") {
              it("raises a `DeserializationError`") {
                val value = JsObject(
                  "name" -> JsString("some-name"),
                  "applicationId" -> JsString("some-app"),
                  "majorVersion" -> JsNumber(1),
                  "minorVersion" -> JsString("")
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
                }

                assert(exception.getMessage == "`minorVersion` is not an int")
              }
            }

            describe("with no `minorVersion` field") {
              it("raises a `DeserializationError`") {
                val value = JsObject(
                  "name" -> JsString("some-name"),
                  "applicationId" -> JsString("some-app"),
                  "majorVersion" -> JsNumber(1)
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
                }

                assert(exception.getMessage == "`minorVersion` is empty")
              }
            }
          }

          describe("with present `majorVersion` that is not a positive int field") {
            it("raises a `DeserializationError`") {
              val value = JsObject(
                "name" -> JsString("some-name"),
                "applicationId" -> JsString("some-app"),
                "majorVersion" -> JsNumber(-1)
              )

              val exception = intercept[DeserializationException] {
                SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
              }

              assert(exception.getMessage == "`majorVersion` is not a valid positive int")
            }
          }

          describe("with present `majorVersion` that is not an int field") {
            it("raises a `DeserializationError`") {
              val value = JsObject(
                "name" -> JsString("some-name"),
                "applicationId" -> JsString("some-app"),
                "majorVersion" -> JsString("example")
              )

              val exception = intercept[DeserializationException] {
                SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
              }

              assert(exception.getMessage == "`majorVersion` is not an int")
            }
          }

          describe("with blank `majorVersion` field") {
            it("raises a `DeserializationError`") {
              val value = JsObject(
                "name" -> JsString("some-name"),
                "applicationId" -> JsString("some-app"),
                "majorVersion" -> JsString("")
              )

              val exception = intercept[DeserializationException] {
                SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
              }

              assert(exception.getMessage == "`majorVersion` is not an int")
            }
          }

          describe("with no `majorVersion` field") {
            it("raises a `DeserializationError`") {
              val value = JsObject(
                "name" -> JsString("some-name"),
                "applicationId" -> JsString("some-app")
              )

              val exception = intercept[DeserializationException] {
                SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
              }

              assert(exception.getMessage == "`majorVersion` is empty")
            }
          }
        }

        describe("with blank `applicationId` ") {
          it("raises a `DeserializationError`") {
            val value = JsObject(
              "name" -> JsString("some-name"),
              "applicationId" -> JsString("   ")
            )

            val exception = intercept[DeserializationException] {
              SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
            }

            assert(exception.getMessage == "`applicationId` is blank/empty")
          }
        }

        describe("with no `applicationId`") {
          it("raises a `DeserializationError`") {
            val value = JsObject(
              "name" -> JsString("some-name")
            )

            val exception = intercept[DeserializationException] {
              SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
            }

            assert(exception.getMessage == "`applicationId` is blank/empty")
          }
        }
      }

      describe("with blank `name` field") {
        it("raises a `DeserializationError`") {
          val value = JsObject(
            "name" -> JsString("")
          )

          val exception = intercept[DeserializationException] {
            SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
          }

          assert(exception.getMessage == "`name` is blank/empty")
        }
      }

      describe("with no `name` field") {
        it("raises a `DeserializationError`") {
          val value = JsObject()

          val exception = intercept[DeserializationException] {
            SchemaRegistryJsonProtocol.SchemaRequestFormat.read(value)
          }

          assert(exception.getMessage == "`name` is blank/empty")
        }
      }
    }
    describe(".write") {
      it("returns a JsObject with `name`, `applicationId`, `majorVersion`, `minorVersion` and `fields") {
        val schemaRequest = ObjectMother.defaultSchemaRequest()
        val field = schemaRequest.fields.head

        val expectedJsObject = JsObject(
          "name" -> JsString(schemaRequest.name),
          "applicationId" -> JsString(schemaRequest.applicationId),
          "majorVersion" -> JsNumber(schemaRequest.majorVersion),
          "minorVersion" -> JsNumber(schemaRequest.minorVersion),
          "fields" -> JsArray(
            JsObject(
              "name" -> JsString(field.name),
              "type" -> JsString(field.`type`.toString),
              "isIdentity" -> JsBoolean(field.isIdentity)
            )
          )
        )
        val resultJsObject = SchemaRegistryJsonProtocol.SchemaRequestFormat.write(schemaRequest)
        assert(expectedJsObject == resultJsObject)
      }
    }
  }

  describe(".FieldFormat") {
    describe(".read") {
      describe("with present `name` field") {
        describe("with `type` that is present and a known `FieldType`") {
          describe("and is a `BOOL`") {
            describe("and has an `isIdentity` field") {
              it("returns a `BoolField` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.BOOL
                val fieldIsIdentity = true
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[BoolField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
              }
            }

            describe("and has no `isIdentity` field") {
              it("returns a `BoolField` with false `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.BOOL
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[BoolField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(!returnedField.isIdentity)
              }
            }
          }

          describe("and is an `ENUM`") {
            describe("and has an `isIdentity` field") {
              it("returns a `EnumField[String]` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.STRING
                val fieldIsIdentity = true
                val allowedValues = List("test1", "test2")
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[EnumField[_]])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
                assert(returnedField.asInstanceOf[EnumField[_]].valueType == valueType)
                assert(returnedField.asInstanceOf[EnumField[_]].allowedValues == allowedValues)
              }

              it("returns an `EnumField[Int]` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.INT
                val fieldIsIdentity = true
                val allowedValues = List(1, 2)
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[EnumField[_]])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
                assert(returnedField.asInstanceOf[EnumField[_]].valueType == valueType)
                assert(returnedField.asInstanceOf[EnumField[_]].allowedValues == allowedValues)
              }

              it("returns an `EnumField[Long]` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.LONG
                val fieldIsIdentity = true
                val allowedValues = List(1000000000000000L, 200000000000000L)
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[EnumField[_]])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
                assert(returnedField.asInstanceOf[EnumField[_]].valueType == valueType)
                assert(returnedField.asInstanceOf[EnumField[_]].allowedValues == allowedValues)
              }

              it("returns an `EnumField[Float]` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.FLOAT
                val fieldIsIdentity = true
                val allowedValues = List(1.10.toFloat, 2.20.toFloat)
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[EnumField[_]])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
                assert(returnedField.asInstanceOf[EnumField[_]].valueType == valueType)
                assert(returnedField.asInstanceOf[EnumField[_]].allowedValues == allowedValues)
              }

              it("returns an `EnumField[Double]` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.DOUBLE
                val fieldIsIdentity = true
                val allowedValues = List(1.10, 2.20)
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[EnumField[_]])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
                assert(returnedField.asInstanceOf[EnumField[_]].valueType == valueType)
                assert(returnedField.asInstanceOf[EnumField[_]].allowedValues == allowedValues)

              }
            }

            describe("and has no `isIdentity` field") {
              it("returns a `EnumField[String]` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.STRING
                val allowedValues = List("test1", "test2")
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[EnumField[_]])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.asInstanceOf[EnumField[_]].valueType == valueType)
                assert(returnedField.asInstanceOf[EnumField[_]].allowedValues == allowedValues)
                assert(!returnedField.isIdentity)
              }

              it("returns an `EnumField[Int]` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.INT
                val allowedValues = List(1, 2)
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[EnumField[_]])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.asInstanceOf[EnumField[_]].valueType == valueType)
                assert(returnedField.asInstanceOf[EnumField[_]].allowedValues == allowedValues)
                assert(!returnedField.isIdentity)
              }

              it("returns an `EnumField[Long]` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.LONG
                val allowedValues = List(1000000000000000L, 200000000000000L)
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[EnumField[_]])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.asInstanceOf[EnumField[_]].valueType == valueType)
                assert(returnedField.asInstanceOf[EnumField[_]].allowedValues == allowedValues)
                assert(!returnedField.isIdentity)
              }

              it("returns an `EnumField[Float]` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.FLOAT
                val allowedValues = List(1.10.toFloat, 2.20.toFloat)
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[EnumField[_]])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.asInstanceOf[EnumField[_]].valueType == valueType)
                assert(returnedField.asInstanceOf[EnumField[_]].allowedValues == allowedValues)
                assert(!returnedField.isIdentity)
              }

              it("returns an `EnumField[Double]` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.DOUBLE
                val allowedValues = List(1.10, 2.20)
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[EnumField[_]])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.asInstanceOf[EnumField[_]].valueType == valueType)
                assert(returnedField.asInstanceOf[EnumField[_]].allowedValues == allowedValues)
                assert(!returnedField.isIdentity)
              }
            }

            describe("and has no `valueType` field") {
              it("raises a `DeserializationError`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val allowedValues = List("test1", "test2")
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                }

                assert(exception.getMessage == s"`valueType` is blank/empty")
              }
            }

            describe("and has wrong `valueType` field") {
              it("raises a `DeserializationError`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.NULL
                val allowedValues = List("test1", "test2")
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                }

                assert(exception.getMessage == s"`${valueType.toString}` is not a known enum field type")
              }
            }

            describe("and has no `allowedValues` field") {
              it("raises a `DeserializationException`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.STRING
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "valueType" -> JsString(valueType.toString)
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                }

                assert(exception.getMessage == s"`allowedValues` is blank/empty")
              }
            }

            describe("and has empty `allowedValues` field") {
              it("raises a `DeserializationException`") {
                val fieldName = "example-field"
                val fieldType = FieldType.ENUM
                val valueType = FieldType.STRING
                val allowedValues = List[String]()
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "valueType" -> JsString(valueType.toString),
                  "allowedValues" -> allowedValues.toJson
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                }

                assert(exception.getMessage == s"`allowedValues` is blank/empty")
              }
            }
          }

          describe("and is an `INT`") {
            describe("and has an `isIdentity` field") {
              it("returns an `IntField` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.INT
                val fieldIsIdentity = true
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[IntField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
              }
            }

            describe("and has no `isIdentity` field") {
              it("returns an `IntField` with false `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.INT
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[IntField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(!returnedField.isIdentity)
              }
            }
          }

          describe("and is a `LONG`") {
            describe("and has an `isIdentity` field") {
              it("returns an `LongField` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.LONG
                val fieldIsIdentity = true
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[LongField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
              }
            }

            describe("and has no `isIdentity` field") {
              it("returns an `LongField` with false `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.LONG
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[LongField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(!returnedField.isIdentity)
              }
            }
          }

          describe("and is a `FLOAT`") {
            describe("and has an `isIdentity` field") {
              it("returns a `FloatField` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.FLOAT
                val fieldIsIdentity = true
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[FloatField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
              }
            }

            describe("and has no `isIdentity` field") {
              it("returns a `FloatField` with false `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.FLOAT
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[FloatField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(!returnedField.isIdentity)
              }
            }
          }

          describe("and is a `DOUBLE`") {
            describe("and has an `isIdentity` field") {
              it("returns a `DoubleField` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.DOUBLE
                val fieldIsIdentity = true
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[DoubleField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
              }
            }

            describe("and has no `isIdentity` field") {
              it("returns a `DoubleField` with false `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.DOUBLE
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[DoubleField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(!returnedField.isIdentity)
              }
            }
          }

          describe("and is a `BYTES`") {
            describe("and has an `isIdentity` field") {
              it("returns a `BytesField` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.BYTES
                val fieldIsIdentity = true
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[BytesField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
              }
            }

            describe("and no specified `isIdentity`") {
              it("returns a `BytesField` with false `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.BYTES
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[BytesField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(!returnedField.isIdentity)
              }
            }
          }

          describe("and is a `STRING`") {
            describe("and has an `isIdentity` field") {
              it("returns a `StringField` with specified `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.STRING
                val fieldIsIdentity = true
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString),
                  "isIdentity" -> JsBoolean(fieldIsIdentity)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[StringField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(returnedField.isIdentity == fieldIsIdentity)
              }
            }

            describe("and has no `isIdentity` field") {
              it("returns a `StringField` with false `isIdentity`") {
                val fieldName = "example-field"
                val fieldType = FieldType.STRING
                val jsObject = JsObject(
                  "name" -> JsString(fieldName),
                  "type" -> JsString(fieldType.toString)
                )

                val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                assert(returnedField.isInstanceOf[StringField])
                assert(returnedField.name == fieldName)
                assert(returnedField.`type` == fieldType)
                assert(!returnedField.isIdentity)
              }
            }
          }

          describe("and is an `ARRAY") {
            describe("with a present `items` that is a known FieldType") {
              describe("and an `isIdentity` field") {
                it("returns an `ArrayField` with specified `items` and `isIdentity` fields") {
                  val fieldName = "example-field"
                  val fieldIsIdentity = true
                  val fieldItems = FieldType.INT

                  val jsObject = JsObject(
                    "name" -> JsString(fieldName),
                    "type" -> JsString(FieldType.ARRAY.toString),
                    "isIdentity" -> JsBoolean(fieldIsIdentity),
                    "items" -> JsString(fieldItems.toString)
                  )
                  val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                  assert(returnedField.isInstanceOf[ArrayField])
                  assert(returnedField.name == fieldName)
                  assert(returnedField.`type` == FieldType.ARRAY)
                  assert(returnedField.isIdentity == fieldIsIdentity)
                  assert(returnedField.items == fieldItems)
                }
              }

              describe("and no `isIdentity` field") {
                it("returns an `ArrayField` with specified `items` and false `isIdentity` fields") {
                  val fieldName = "example-field"
                  val fieldItems = FieldType.INT

                  val jsObject = JsObject(
                    "name" -> JsString(fieldName),
                    "type" -> JsString(FieldType.ARRAY.toString),
                    "items" -> JsString(fieldItems.toString)
                  )
                  val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                  assert(returnedField.isInstanceOf[ArrayField])
                  assert(returnedField.name == fieldName)
                  assert(returnedField.`type` == FieldType.ARRAY)
                  assert(!returnedField.isIdentity)
                  assert(returnedField.items == fieldItems)
                }
              }
            }

            describe("with a present `items` that is not a known FieldType") {
              it("raises a `DeserializationError`") {
                val itemsName = "not-valid"
                val value = JsObject(
                  "name" -> JsString("example-field"),
                  "type" -> JsString(FieldType.ARRAY.toString),
                  "isIdentity" -> JsBoolean(true),
                  "items" -> JsString(itemsName)
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.FieldFormat.read(value)
                }

                assert(exception.getMessage == s"`$itemsName` is not a known field type")
              }
            }

            describe("with a blank `items`") {
              it("raises a `DeserializationError`") {
                val value = JsObject(
                  "name" -> JsString("example-field"),
                  "type" -> JsString(FieldType.ARRAY.toString),
                  "isIdentity" -> JsBoolean(true),
                  "items" -> JsString("")
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.FieldFormat.read(value)
                }

                assert(exception.getMessage == s"`items` is blank/empty")
              }
            }

            describe("with no `items`") {
              it("raises a `DeserializationError`") {
                val value = JsObject(
                  "name" -> JsString("example-field"),
                  "type" -> JsString(FieldType.ARRAY.toString),
                  "isIdentity" -> JsBoolean(false)
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.FieldFormat.read(value)
                }

                assert(exception.getMessage == s"`items` is blank/empty")
              }
            }
          }

          describe("and is a `RECORD") {
            describe("and `fields` is a present array and has at least 1 field") {
              describe("and `isIdentity` field") {
                it("returns a `RecordField` with specified `fields` and `isIdentity` fields") {
                  val fieldName = "example-field"
                  val fieldIsIdentity = true
                  val intFieldName = "example-int-field"
                  val intFieldIsIdentity = true

                  val jsObject = JsObject(
                    "name" -> JsString(fieldName),
                    "type" -> JsString(FieldType.RECORD.toString),
                    "isIdentity" -> JsBoolean(fieldIsIdentity),
                    "fields" -> JsArray(
                      JsObject(
                        "name" -> JsString(intFieldName),
                        "type" -> JsString(FieldType.INT.toString),
                        "isIdentity" -> JsBoolean(intFieldIsIdentity)
                      )
                    )
                  )

                  val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                  assert(returnedField.isInstanceOf[RecordField])
                  assert(returnedField.name == fieldName)
                  assert(returnedField.`type` == FieldType.RECORD)
                  assert(returnedField.fields.length == 1)
                  assert(returnedField.isIdentity == fieldIsIdentity)

                  val nestedField = returnedField.fields.head
                  assert(nestedField.isInstanceOf[IntField])
                  assert(nestedField.name == intFieldName)
                  assert(nestedField.`type` == FieldType.INT)
                  assert(nestedField.isIdentity == intFieldIsIdentity)
                }
              }

              describe("and no `isIdentity` field") {
                it("returns a `RecordField` with specified `fields` and false `isIdentity` fields") {
                  val fieldName = "example-field"
                  val intFieldName = "example-int-field"
                  val intFieldIsIdentity = true

                  val jsObject = JsObject(
                    "name" -> JsString(fieldName),
                    "type" -> JsString(FieldType.RECORD.toString),
                    "fields" -> JsArray(
                      JsObject(
                        "name" -> JsString(intFieldName),
                        "type" -> JsString(FieldType.INT.toString),
                        "isIdentity" -> JsBoolean(intFieldIsIdentity)
                      )
                    )
                  )

                  val returnedField = SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                  assert(returnedField.isInstanceOf[RecordField])
                  assert(returnedField.name == fieldName)
                  assert(returnedField.`type` == FieldType.RECORD)
                  assert(returnedField.fields.length == 1)
                  assert(!returnedField.isIdentity)

                  val nestedField = returnedField.fields.head
                  assert(nestedField.isInstanceOf[IntField])
                  assert(nestedField.name == intFieldName)
                  assert(nestedField.`type` == FieldType.INT)
                  assert(nestedField.isIdentity == intFieldIsIdentity)
                }
              }
            }

            describe("and `fields` is present array and does not have at least 1 field") {
              it("raises `DeserializationError`") {
                val jsObject = JsObject(
                  "name" -> JsString("example-field"),
                  "type" -> JsString(FieldType.RECORD.toString),
                  "isIdentity" -> JsBoolean(false),
                  "fields" -> JsArray()
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                }

                assert(exception.getMessage == "`fields` is empty")
              }
            }

            describe("and `fields` is not an array") {
              it("raises `DeserializationError`") {
                val jsObject = JsObject(
                  "name" -> JsString("example-field"),
                  "type" -> JsString(FieldType.RECORD.toString),
                  "isIdentity" -> JsBoolean(false),
                  "fields" -> JsString("wrong")
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                }

                assert(exception.getMessage == "Invalid `fields` for record")
              }
            }

            describe("and no `fields`") {
              it("raises `DeserializationError`") {
                val jsObject = JsObject(
                  "name" -> JsString("example-field"),
                  "type" -> JsString(FieldType.RECORD.toString),
                  "isIdentity" -> JsBoolean(false)
                )

                val exception = intercept[DeserializationException] {
                  SchemaRegistryJsonProtocol.FieldFormat.read(jsObject)
                }

                assert(exception.getMessage == "No `fields` for record")
              }
            }
          }
        }
        describe("with `type` that is present and not a known `FieldType`") {
          it("raises a `DeserializationError`") {
            val typeName = "not-valid"
            val value = JsObject(
              "name" -> JsString("example-field"),
              "type" -> JsString(typeName)
            )

            val exception = intercept[DeserializationException] {
              SchemaRegistryJsonProtocol.FieldFormat.read(value)
            }

            assert(exception.getMessage == s"`$typeName` is not a known field type")
          }
        }

        describe("with blank `type`") {
          it("raises a `DeserializationError`") {
            val value = JsObject(
              "name" -> JsString("example-field"),
              "type" -> JsString("")
            )

            val exception = intercept[DeserializationException] {
              SchemaRegistryJsonProtocol.FieldFormat.read(value)
            }

            assert(exception.getMessage == "`type` is blank/empty")
          }
        }

        describe("with no `type`") {
          it("raises a `DeserializationError`") {
            val value = JsObject(
              "name" -> JsString("example-field")
            )

            val exception = intercept[DeserializationException] {
              SchemaRegistryJsonProtocol.FieldFormat.read(value)
            }

            assert(exception.getMessage == "`type` is blank/empty")
          }
        }
      }

      describe("with blank `name` field") {
        it("raises a `DeserializationError`") {
          val value = JsObject(
            "name" -> JsString("")
          )

          val exception = intercept[DeserializationException] {
            SchemaRegistryJsonProtocol.FieldFormat.read(value)
          }

          assert(exception.getMessage == "`name` is blank/empty")
        }
      }

      describe("with no `name` field") {
        it("raises a `DeserializationError`") {
          val value = JsObject()

          val exception = intercept[DeserializationException] {
            SchemaRegistryJsonProtocol.FieldFormat.read(value)
          }

          assert(exception.getMessage == "`name` is blank/empty")
        }
      }
    }

    describe(".write") {
      describe("with a `BOOL`") {
        it("returns a Js Object with `name` and `type`") {
          val field = BoolField("example-field")

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with an `ENUM[String]`") {
        it("returns a Js Object with `name`, `type`, `valueType` and `allowedValues`") {
          val allowedValues = List("test1", "test2")
          val valueType = FieldType.STRING
          val field = EnumField("example-field", allowedValues, valueType)

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "valueType" -> JsString(field.valueType.toString),
            "allowedValues" -> field.allowedValues.toJson,
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with an `ENUM[Int]`") {
        it("returns a Js Object with `name`, `type`, `valueType` and `allowedValues`") {
          val allowedValues = List(1, 2)
          val valueType = FieldType.INT
          val field = EnumField("example-field", allowedValues, valueType)

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "valueType" -> JsString(field.valueType.toString),
            "allowedValues" -> field.allowedValues.toJson,
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with an `ENUM[Long]`") {
        it("returns a Js Object with `name`, `type`, `valueType` and `allowedValues`") {
          val allowedValues = List(100000000000L, 20000000000L)
          val valueType = FieldType.LONG
          val field = EnumField("example-field", allowedValues, valueType)

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "valueType" -> JsString(field.valueType.toString),
            "allowedValues" -> field.allowedValues.toJson,
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with an `ENUM[Float]`") {
        it("returns a Js Object with `name`, `type`, `valueType` and `allowedValues`") {
          val allowedValues = List(1.1.toFloat, 2.2.toFloat)
          val valueType = FieldType.FLOAT
          val field = EnumField("example-field", allowedValues, valueType)

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "valueType" -> JsString(field.valueType.toString),
            "allowedValues" -> field.allowedValues.toJson,
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with an `ENUM[Double]`") {
        it("returns a Js Object with `name`, `type`, `valueType` and `allowedValues`") {
          val allowedValues = List(1.1, 2.2)
          val valueType = FieldType.DOUBLE
          val field = EnumField("example-field", allowedValues, valueType)

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "valueType" -> JsString(field.valueType.toString),
            "allowedValues" -> field.allowedValues.toJson,
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with an `INT`") {
        it("returns a Js Object with `name` and `type`") {
          val field = IntField("example-field")

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with a `LONG`") {
        it("returns a Js Object with `name` and `type`") {
          val field = LongField("example-field")

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with a `FLOAT`") {
        it("returns a Js Object with `name` and `type`") {
          val field = FloatField("example-field")

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with a `DOUBLE`") {
        it("returns a Js Object with `name` and `type`") {
          val field = DoubleField("example-field")

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with a `BYTES`") {
        it("returns a Js Object with `name` and `type`") {
          val field = BytesField("example-field")

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "isIdentity" -> JsBoolean(field.isIdentity)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with an `ARRAY") {
        it("returns a Js Object with `name`, `type` and `items`") {
          val field = ArrayField("example-field", FieldType.INT)

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "isIdentity" -> JsBoolean(field.isIdentity),
            "items" -> JsString(field.items.toString)
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }

      describe("with a `RECORD`") {
        it("returns a Js Object with `name`, `type` and `fields`") {
          val intField = IntField("example-int-field")
          val stringField = StringField("example-string-field")
          val fields = List(intField, stringField)
          val field = RecordField("example-field", fields)

          val expectedJsObject = JsObject(
            "name" -> JsString(field.name),
            "type" -> JsString(field.`type`.toString),
            "isIdentity" -> JsBoolean(field.isIdentity),
            "fields" -> JsArray(
              JsObject(
                "name" -> JsString(intField.name),
                "type" -> JsString(intField.`type`.toString),
                "isIdentity" -> JsBoolean(intField.isIdentity)
              ),
              JsObject(
                "name" -> JsString(stringField.name),
                "type" -> JsString(stringField.`type`.toString),
                "isIdentity" -> JsBoolean(stringField.isIdentity)
              )
            )
          )
          val returnedJsObject = SchemaRegistryJsonProtocol.FieldFormat.write(field)
          assert(expectedJsObject == returnedJsObject)
          assert(returnedJsObject.isInstanceOf[JsObject])
        }
      }
    }
  }

  describe(".floatTypeFormat") {
    describe(".read") {
      it("returns a `Float` number rounded to 4 digits after the decimal point") {
        val formattedJsNumber = SchemaRegistryJsonProtocol.floatTypeFormat.read(JsNumber("10.12341234"))
        val expectedFloatNumber = 10.1234.toFloat

        assert(formattedJsNumber == expectedFloatNumber)
      }
    }

    describe(".write") {
      it("returns a `JsNumber` number rounded to 4 digits after the decimal point") {
        val formattedFloatNumber = SchemaRegistryJsonProtocol.floatTypeFormat.write(10.12341234.toFloat)
        val expectedJsNumber = JsNumber("10.1234")

        assert(formattedFloatNumber == expectedJsNumber)
      }
    }
  }

  describe(".doubleTypeFormat") {
    describe(".read") {
      it("returns a `Double` number rounded to 8 digits after the decimal point") {
        val formattedJsNumber = SchemaRegistryJsonProtocol.doubleTypeFormat.read(JsNumber("10.123412341234"))
        val expectedDoubleNumber = 10.12341234

        assert(formattedJsNumber == expectedDoubleNumber)
      }
    }

    describe(".write") {
      it("returns a `JsNumber` number rounded to 8 digits after the decimal point") {
        val formattedDoubleNumber = SchemaRegistryJsonProtocol.doubleTypeFormat.write(10.123412341234)
        val expectedJsNumber = JsNumber("10.12341234")

        assert(formattedDoubleNumber == expectedJsNumber)
      }
    }
  }


  describe(".schemaFormat") {
    describe(".read") {
      it("returns a `Schema`") {
        val name = "example-schema"
        val applicationId = "example-app"
        val majorVersion = 2
        val minorVersion = 11

        val fieldName = "example-field"
        val fieldType = FieldType.INT
        val isIdentity = true
        val jsObject = JsObject(
          "name" -> JsString(name),
          "applicationId" -> JsString(applicationId),
          "majorVersion" -> JsNumber(majorVersion),
          "minorVersion" -> JsNumber(minorVersion),
          "fields" -> JsArray(
            JsObject(
              "name" -> JsString(fieldName),
              "type" -> JsString(fieldType.toString),
              "isIdentity" -> JsBoolean(isIdentity)
            )
          )
        )

        val result = SchemaRegistryJsonProtocol.schemaFormat.read(jsObject)
        assert(result.isInstanceOf[Schema])
        assert(result.name == name)
        assert(result.applicationId == applicationId)
        assert(result.majorVersion == majorVersion)
        assert(result.minorVersion == minorVersion)
        assert(result.fields.length == 1)

        val field = result.fields.head
        assert(field.isInstanceOf[Field])
        assert(field.name == fieldName)
        assert(field.`type` == fieldType)
        assert(field.isIdentity == isIdentity)
      }
    }

    describe(".write") {
      it("returns a `JsObject`") {
        val schema = ObjectMother.defaultSchema()
        val field = schema.fields.head

        val expectedJsObject = JsObject(
          "_id" -> JsObject(
            "$id" -> JsNumber(schema._id.get.id),
            "timestamp" -> JsString(schema._id.get.timestamp.toString)
          ),
          "name" -> JsString(schema.name),
          "applicationId" -> JsString(schema.applicationId),
          "majorVersion" -> JsNumber(schema.majorVersion),
          "minorVersion" -> JsNumber(schema.minorVersion),
          "fields" -> JsArray(
            JsObject(
              "name" -> JsString(field.name),
              "type" -> JsString(field.`type`.toString),
              "isIdentity" -> JsBoolean(field.isIdentity)
            )
          )
        )

        assert(SchemaRegistryJsonProtocol.schemaFormat.write(schema) == expectedJsObject)
      }
    }
  }

  describe(".consumerRequestFormat") {
    describe(".read") {
      it("returns a `ConsumerRequest`") {
        val name = "example-consumer"
        val schemaName = "example-schema"
        val schemaMajorVersion = 2
        val schemaMinorVersion = 11
        val jsObject = JsObject(
          "name" -> JsString(name),
          "schemaName" -> JsString(schemaName),
          "schemaMajorVersion" -> JsNumber(schemaMajorVersion),
          "schemaMinorVersion" -> JsNumber(schemaMinorVersion)
        )

        val result = SchemaRegistryJsonProtocol.consumerRequestFormat.read(jsObject)
        assert(result.isInstanceOf[ConsumerRequest])
        assert(result.name == name)
        assert(result.schemaName == schemaName)
        assert(result.schemaMajorVersion == schemaMajorVersion)
        assert(result.schemaMinorVersion == schemaMinorVersion)
      }
    }

    describe(".write") {
      it("returns a `JsObject`") {
        val consumerRequest = ObjectMother.defaultConsumerRequest()
        val expectedJsObject = JsObject(
          "name" -> JsString(consumerRequest.name),
          "schemaName" -> JsString(consumerRequest.schemaName),
          "schemaMajorVersion" -> JsNumber(consumerRequest.schemaMajorVersion),
          "schemaMinorVersion" -> JsNumber(consumerRequest.schemaMinorVersion)
        )

        assert(
          SchemaRegistryJsonProtocol.consumerRequestFormat.write(consumerRequest) == expectedJsObject
        )
      }
    }
  }

  describe(".consumerFormat") {
    describe(".read") {
      it("returns a `Consumer`") {
        val name = "example-consumer"
        val schemaName = "example-schema"
        val schemaMajorVersion = 2
        val schemaMinorVersion = 11
        val jsObject = JsObject(
          "name" -> JsString(name),
          "schemaName" -> JsString(schemaName),
          "schemaMajorVersion" -> JsNumber(schemaMajorVersion),
          "schemaMinorVersion" -> JsNumber(schemaMinorVersion)
        )

        val result = SchemaRegistryJsonProtocol.consumerFormat.read(jsObject)
        assert(result.isInstanceOf[Consumer])
        assert(result.name == name)
        assert(result.schemaName == schemaName)
        assert(result.schemaMajorVersion == schemaMajorVersion)
        assert(result.schemaMinorVersion == schemaMinorVersion)
      }
    }

    describe(".write") {
      it("returns a `JsObject`") {
        val consumer = ObjectMother.defaultConsumer()
        val expectedJsObject = JsObject(
          "_id" -> SchemaRegistryJsonProtocol.ShortObjectIdFormat.write(consumer._id.get),
          "name" -> JsString(consumer.name),
          "schemaName" -> JsString(consumer.schemaName),
          "schemaMajorVersion" -> JsNumber(consumer.schemaMajorVersion),
          "schemaMinorVersion" -> JsNumber(consumer.schemaMinorVersion)
        )

        assert(SchemaRegistryJsonProtocol.consumerFormat.write(consumer) == expectedJsObject)
      }
    }
  }
}
