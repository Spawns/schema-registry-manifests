package com.sumup.unit.serde

import com.mongodb.{MongoWriteException, WriteError}
import com.sumup.UtilsService
import com.sumup.diff.entities.SchemaDiffResult
import com.sumup.dto.fields._
import com.sumup.dto.requests.SchemaRequest
import com.sumup.dto.responses.custom.MongoExceptionResponse
import com.sumup.dto.responses.standard.{BadRequestResponse, CreatedResponse, NotFoundResponse}
import com.sumup.dto.{Consumer, FieldType, Schema}
import com.sumup.json.SchemaRegistryJsonProtocol
import com.sumup.serde.SerializationService
import gnieh.diffson.Pointer
import org.mockito.Mockito
import org.mongodb.scala.ServerAddress
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Outcome, fixture}
import com.sumup.testutils.ObjectMother
import com.sumup.testutils.builders.{ConsumerBuilder, SchemaBuilder}

class SerializationServiceSpec extends fixture.FunSpec with MockitoSugar {
  import com.sumup.json.SchemaRegistryJsonProtocol._
  import gnieh.diffson.sprayJson.DiffsonProtocol.OperationFormat
  import gnieh.diffson.sprayJson._
  import spray.json._

  type FixtureParam = SerializationService
  val utilsService = new UtilsService()

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = new SerializationService()(utilsService)
    test(fixture)
  }

  describe(".serializeOperation") {
    it("is an `OperationFormat.write`") { serializationService =>
      val value = JsObject(
        "name" -> JsString("example-field"),
        "type" -> JsString("int")
      )

      val path = Pointer.root
      val addOperation = Add(
        path,
        value
      )

      val expectedAddJsObject = JsObject(
        "op" -> JsString("add"),
        "path" -> JsString(path.toString),
        "value" -> value
      )

      assert(OperationFormat.write(addOperation) == expectedAddJsObject)
      assert(serializationService.serializeOperation(addOperation) == expectedAddJsObject)

      val removeOperation = Remove(
        path,
        Some(value)
      )

      val expectedRemoveJsObject = JsObject(
        "op" -> JsString("remove"),
        "path" -> JsString(path.toString),
        "old" -> value
      )

      assert(OperationFormat.write(removeOperation) == expectedRemoveJsObject)
      assert(serializationService.serializeOperation(removeOperation) == expectedRemoveJsObject)

      val newValue = JsObject(
        "name" -> JsString("example-int-field"),
        "type" -> JsString("int")
      )

      val replaceOperation = Replace(
        path,
        newValue,
        Some(value)
      )

      val expectedReplaceJsObject = JsObject(
        "op" -> JsString("replace"),
        "path" -> JsString(path.toString),
        "value" -> newValue,
        "old" -> value
      )

      assert(OperationFormat.write(replaceOperation) == expectedReplaceJsObject)
      assert(serializationService.serializeOperation(replaceOperation) == expectedReplaceJsObject)
    }
  }

  describe(".serializeIsCreated") {
    describe("with true `isCreated") {
      it("returns a `JsObject` of `CreatedResponse") { serializationService =>
        assert(
          serializationService.serializeIsCreated(true) == SchemaRegistryJsonProtocol.createdResponseFormat.write(CreatedResponse()).prettyPrint
        )
      }
    }

    describe("with false `isCreated") {
      it("returns a `JsObject` of `BadRequestResponse") { serializationService =>
        assert(
          serializationService.serializeIsCreated(false) == SchemaRegistryJsonProtocol.badRequestResponseFormat.write(BadRequestResponse()).prettyPrint
        )
      }
    }
  }

  describe(".serializeFieldsSorted") {
    it("uses `sortFields` result and `serialize`s") { _ =>
      val mockUtilsService = mock[UtilsService]
      val mockSerializationService = new SerializationService()(mockUtilsService)
      val fieldsDouble = List(IntField("example-field"))

      Mockito.when(mockUtilsService.sortFields(fieldsDouble)).thenReturn(fieldsDouble)
      mockSerializationService.serializeFieldsSorted(fieldsDouble)
      Mockito.verify(mockUtilsService).sortFields(fieldsDouble)
      Mockito.verifyNoMoreInteractions(mockUtilsService)
    }
  }

  describe(".serialize") {
    describe("with a `Consumer`") {
      it("returns a `ConsumerFormat`-ted string")  { serializationService =>
        val consumer = ObjectMother.defaultConsumer()
        assert(
          serializationService.serialize(consumer) ==
            SchemaRegistryJsonProtocol.consumerFormat.write(consumer).prettyPrint
        )
      }
    }

    describe("with a `Schema`") {
      it("returns a `SchemaFormat`-ted string")  { serializationService =>
        val schema = ObjectMother.defaultSchema()
        assert(
          serializationService.serialize(schema) ==
            SchemaRegistryJsonProtocol.schemaFormat.write(schema).prettyPrint
        )
      }
    }

    describe("with a `SchemaRequest`") {
      it("returns a `SchemaRequestFormat`-ted string")  { serializationService =>
        val schemaRequest = ObjectMother.defaultSchemaRequest()
        assert(
          serializationService.serialize(schemaRequest) ==
            SchemaRegistryJsonProtocol.SchemaRequestFormat.write(schemaRequest).prettyPrint
        )
      }
    }

    describe("with a `SchemaDiffResult`") {
      it("returns a `SchemaDiffResultFormat`-ted string")  { serializationService =>
        val schemaDiffResult = ObjectMother.defaultSchemaDiffResultForUpgradableSchema()
        assert(
          serializationService.serialize(schemaDiffResult) ==
            SchemaRegistryJsonProtocol.schemaDiffResultFormat.write(schemaDiffResult).prettyPrint
        )
      }
    }

    describe("with a `JsValue`") {
      it("does not raise a `MatchError`")  { serializationService =>
        serializationService.serialize(JsString("foobar"))
      }
    }

    describe("with a `NotFoundResponse`") {
      it("returns a `NotFoundResponseFormat`-ted string") { serializationService =>
        val response = NotFoundResponse()
        assert(
          serializationService.serialize(response) ==
            SchemaRegistryJsonProtocol.notFoundResponseFormat.write(response).prettyPrint.toString
        )
      }
    }

    describe("with a `MongoExceptionResponse`") {
      it("returns a `MongoExceptionResponseFormat`-ted string") { serializationService =>
        val exception = new MongoWriteException(
          new WriteError(1, "test", BsonDocument()),
          ServerAddress()
        )
        val response = MongoExceptionResponse(exception)

        assert(
          serializationService.serialize(response) ==
            SchemaRegistryJsonProtocol.mongoExceptionResponseFormat.write(response).prettyPrint.toString
        )
      }
    }

    describe("with a sequence of `Schema`") {
      it("returns a `SchemaFormat`-ted sequence") { serializationService =>
        val intField = IntField("example-field")
        val stringField = StringField("example-string-field")
        val recordField = RecordField(
          "example-record-field",
          List(
            ArrayField("example-array-field", FieldType.INT)
          )
        )

        val simpleSchema = SchemaBuilder.aSchema().withName("simple-schema").withFields(List(intField, stringField)).build()
        val complexSchema = SchemaBuilder.aSchema().withName("complex-schema").withFields(List(recordField)).build()
        val schemas = Seq[Schema](
          simpleSchema,
          complexSchema
        )

        assert(
          serializationService.serialize(schemas) ==
            JsArray(
              SchemaRegistryJsonProtocol.schemaFormat.write(simpleSchema),
              SchemaRegistryJsonProtocol.schemaFormat.write(complexSchema)
            ).toJson.prettyPrint
        )
      }
    }

    describe("with a sequence of `Field`") {
      it("returns a `FieldFormat`-ted sequence") { serializationService =>
        val intField = IntField("example-field")
        val stringField = StringField("example-string-field")
        val recordField = RecordField(
          "example-record-field",
          List(
            ArrayField("example-array-field", FieldType.INT)
          )
        )

        val fields = Seq[Field](
          recordField,
          intField,
          stringField
        )

        assert(
          serializationService.serialize(fields) ==
            JsArray(
              SchemaRegistryJsonProtocol.FieldFormat.write(recordField),
              SchemaRegistryJsonProtocol.FieldFormat.write(intField),
              SchemaRegistryJsonProtocol.FieldFormat.write(stringField)
            ).toJson.prettyPrint
        )
      }
    }

    describe("with a sequence of `Consumer`") {
      it("returns a `ConsumerFormat`-ted sequence") { serializationService =>
        val consumerOne = ConsumerBuilder.aConsumer().withSchemaMinorVersion(1).build()
        val consumerTwo = ConsumerBuilder.aConsumer().withSchemaMinorVersion(2).build()
        val consumers = Seq[Consumer](
          consumerOne,
          consumerTwo
        )

        assert(
          serializationService.serialize(consumers) ==
            JsArray(
              SchemaRegistryJsonProtocol.consumerFormat.write(consumerOne),
              SchemaRegistryJsonProtocol.consumerFormat.write(consumerTwo)
            ).toJson.prettyPrint
        )
      }
    }
  }
}
