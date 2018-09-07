package com.sumup.json

import com.sumup.diff.entities._
import com.sumup.diff.enums.FieldChangeOperationType
import com.sumup.dto.fields._
import com.sumup.dto.requests.{ConsumerRequest, SchemaRequest}
import com.sumup.dto.responses.custom.MongoExceptionResponse
import com.sumup.dto.responses.standard.{BadRequestResponse, CreatedResponse, InternalServerErrorResponse, NotFoundResponse}
import com.sumup.dto.{Consumer, FieldType, Schema, ShortObjectId}
import spray.json.DefaultJsonProtocol

object SchemaRegistryJsonProtocol extends DefaultJsonProtocol {
  import spray.json._
  import spray.json.{DeserializationException, JsFalse, JsTrue, JsValue, deserializationError}

  class EnumJsonFormat[T <: Enumeration](enum: T) extends RootJsonFormat[T#Value] {
    override def write(obj: T#Value): JsValue = JsString(obj.toString)
    override def read(json: JsValue): T#Value = {
      json match {
        case JsString(string) =>
          enum.withName(string)
        case somethingElse =>
          throw DeserializationException(s"Expected a value from enum $enum instead of $somethingElse")
      }
    }
  }

  implicit val fieldTypeFormat: EnumJsonFormat[FieldType.type] = new EnumJsonFormat(FieldType)
  implicit object ShortObjectIdFormat extends RootJsonFormat[ShortObjectId] {
    // NOTE: There are no cases where `ObjectId` is sent to the schema registry.
    override def read(json: JsValue): Nothing = deserializationError("Not supported")

    override def write(obj: ShortObjectId): JsObject = {
      JsObject(
        "$id" -> JsNumber(obj.id),
        // NOTE: ISO-8601 string
        "timestamp" -> JsString(obj.timestamp.toString)
      )
    }
  }

  implicit object SchemaRequestFormat extends RootJsonFormat[SchemaRequest] {
    override def read(json: JsValue): SchemaRequest = {
      val jsObject = json.asJsObject
      val jsFields = jsObject.fields

      val name = JsValidationHelpers.getPresentStringFieldOrThrow("name", jsFields.get("name"))
      val applicationId = JsValidationHelpers.getPresentStringFieldOrThrow(
        "applicationId",
        jsFields.get("applicationId")
      )
      val majorVersion = JsValidationHelpers.getPositiveIntOrThrow("majorVersion", jsFields.get("majorVersion"))
      val minorVersion = JsValidationHelpers.getPositiveIntOrThrow("minorVersion", jsFields.get("minorVersion"))

      val fields = jsFields.get("fields")
        .map(_.convertTo[List[JsValue]])
        .getOrElse(List[JsValue]())
        .map(jsValue => FieldFormat.read(jsValue))

      if (fields.length < 1) {
        deserializationError("`fields` is empty")
      }

      SchemaRequest(name, applicationId, majorVersion, minorVersion, fields)
    }

    override def write(obj: SchemaRequest): JsValue = {
      val fields = obj.fields.map(f => FieldFormat.write(f))

      JsObject(
        "name" -> JsString(obj.name),
        "applicationId" -> JsString(obj.applicationId),
        "majorVersion" -> JsNumber(obj.majorVersion),
        "minorVersion" -> JsNumber(obj.minorVersion),
        "fields" -> JsArray(fields.toVector)
      )
    }
  }

  implicit object FieldFormat extends RootJsonFormat[Field] {
    override def read(json: JsValue): Field = {
      val jsObject = json.asJsObject
      val jsFields = jsObject.fields

      val name = JsValidationHelpers.getPresentStringFieldOrThrow("name", jsFields.get("name"))
      val fieldType = JsValidationHelpers.getPresentAndKnownFieldTypeOrThrow(
        "type",
        jsFields.get("type")
      )
      val isIdentity = JsValidationHelpers.getBooleanFieldOrThrow(
        "isIdentity",
        jsFields.get("isIdentity"),
        defaultValue = false
      )

      fieldType match {
        case FieldType.BOOL => BoolField(name, isIdentity)
        case FieldType.INT => IntField(name, isIdentity)
        case FieldType.LONG => LongField(name, isIdentity)
        case FieldType.FLOAT => FloatField(name, isIdentity)
        case FieldType.DOUBLE => DoubleField(name, isIdentity)
        case FieldType.BYTES => BytesField(name, isIdentity)
        case FieldType.STRING => StringField(name, isIdentity)
        case FieldType.ENUM =>
          val valueType = JsValidationHelpers.getPresentAndKnownEnumFieldTypeOrThrow(
            "valueType",
            jsFields.get("valueType")
          )
          valueType match {
            case FieldType.STRING => EnumField[String](
              name,
              JsValidationHelpers.getPresentListOrThrow[String](
                "allowedValues",
                jsFields.get("allowedValues")
              ),
              valueType,
              isIdentity
            )
            case FieldType.INT => EnumField[Int](
              name,
              JsValidationHelpers.getPresentListOrThrow[Int](
                "allowedValues",
                jsFields.get("allowedValues")
              ),
              valueType,
              isIdentity
            )
            case FieldType.FLOAT => EnumField[Float](
              name,
              JsValidationHelpers.getPresentListOrThrow[Float](
                "allowedValues",
                jsFields.get("allowedValues")
              ),
              valueType,
              isIdentity
            )
            case FieldType.DOUBLE => EnumField[Double](
              name,
              JsValidationHelpers.getPresentListOrThrow[Double](
                "allowedValues",
                jsFields.get("allowedValues")
              ),
              valueType,
              isIdentity
            )
            case FieldType.LONG => EnumField[Long](
              name,
              JsValidationHelpers.getPresentListOrThrow[Long](
                "allowedValues",
                jsFields.get("allowedValues")
              ),
              valueType,
              isIdentity
            )
          }
        case FieldType.ARRAY =>
          ArrayField(
            name,
            JsValidationHelpers.getPresentAndKnownFieldTypeOrThrow(
              "items",
              jsFields.get("items")
            ),
            isIdentity
          )
        case FieldType.RECORD =>
          val fieldsOption = jsFields.get("fields")

          fieldsOption match {
            case Some(JsArray(jsArrayFields)) =>
              if (jsArrayFields.length < 1) {
                deserializationError("`fields` is empty")
              }
              RecordField(
                name,
                jsArrayFields.map(f => FieldFormat.read(f)).toList,
                isIdentity
              )
            case None =>
              deserializationError("No `fields` for record")
            case _ =>
              deserializationError("Invalid `fields` for record")
          }
      }
    }

    override def write(obj: Field): JsValue = {
      obj.`type` match {
        case FieldType.BOOL | FieldType.BYTES | FieldType.DOUBLE | FieldType.FLOAT | FieldType.INT | FieldType.LONG | FieldType.STRING =>
          JsObject(
            "name" -> JsString(obj.name),
            "type" -> fieldTypeFormat.write(obj.`type`),
            "isIdentity" -> JsBoolean(obj.isIdentity)
          )
        case FieldType.ARRAY =>
          JsObject(
            "name" -> JsString(obj.name),
            "type" -> fieldTypeFormat.write(obj.`type`),
            "isIdentity" -> JsBoolean(obj.isIdentity),
            "items" -> fieldTypeFormat.write(obj.items)
          )
        case FieldType.ENUM =>
          val valueType = obj.asInstanceOf[EnumField[_]].valueType
          val allowedValues = valueType match {
            case FieldType.STRING => obj.asInstanceOf[EnumField[String]].allowedValues.toJson
            case FieldType.INT => obj.asInstanceOf[EnumField[Int]].allowedValues.toJson
            case FieldType.LONG => obj.asInstanceOf[EnumField[Long]].allowedValues.toJson
            case FieldType.DOUBLE => obj.asInstanceOf[EnumField[Double]].allowedValues.toJson
            case FieldType.FLOAT => obj.asInstanceOf[EnumField[Float]].allowedValues.toJson
          }
          JsObject(
            "name" -> JsString(obj.name),
            "type" -> fieldTypeFormat.write(obj.`type`),
            "isIdentity" -> JsBoolean(obj.isIdentity),
            "valueType" -> JsString(valueType.toString),
            "allowedValues" -> allowedValues
          )
        case FieldType.RECORD =>
          JsObject(
            "name" -> JsString(obj.name),
            "type" -> fieldTypeFormat.write(obj.`type`),
            "fields" -> obj.fields.toJson,
            "isIdentity" -> JsBoolean(obj.isIdentity)
          )
      }
    }
  }

  implicit object OperationFormat extends RootJsonFormat[Operation] {
    override def write(obj: Operation): JsValue = {
      obj match {
        case add: Add =>
          JsObject(
            "operation" -> JsString(add.operation.toString),
            "path" -> JsString(add.path),
            "value" -> add.value
          )
        case copy: Copy =>
          JsObject(
            "operation" -> JsString(copy.operation.toString),
            "from" -> JsString(copy.from),
            "path" -> JsString(copy.path)
          )
        case move: Move =>
          JsObject(
            "operation" -> JsString(move.operation.toString),
            "from" -> JsString(move.from),
            "path" -> JsString(move.path)
          )
        case remove: Remove =>
          JsObject(
            "operation" -> JsString(remove.operation.toString),
            "path" -> JsString(remove.path),
            "oldValue" -> remove.oldValue.getOrElse(JsNull)
          )
        case replace: Replace =>
          JsObject(
            "operation" -> JsString(replace.operation.toString),
            "path" -> JsString(replace.path),
            "value" -> replace.value,
            "oldValue" -> replace.oldValue.getOrElse(JsNull)
          )
        case test: Test =>
          JsObject(
            "operation" -> JsString(test.operation.toString),
            "path" -> JsString(test.path),
            "value" -> test.value
          )
        case operation: Operation =>
          JsObject(
            "operation" -> JsString(operation.operation.toString),
            "path" -> JsString(operation.path)
          )
      }
    }

    override def read(json: JsValue): Operation = new Operation(FieldChangeOperationType.TEST, "Not supported.")
  }

  class FloatJsonFormat extends JsonFormat[Float] {
    def write(x: Float) = JsNumber(BigDecimal(x).setScale(4, BigDecimal.RoundingMode.HALF_EVEN))
    def read(value: JsValue) = DefaultJsonProtocol.BigDecimalJsonFormat.read(value).setScale(4, BigDecimal.RoundingMode.HALF_EVEN).toFloat
  }

  class DoubleJsonFormat extends JsonFormat[Double] {
    def write(x: Double) = JsNumber(BigDecimal(x).setScale(8, BigDecimal.RoundingMode.HALF_EVEN))
    def read(value: JsValue) = DefaultJsonProtocol.BigDecimalJsonFormat.read(value).setScale(8, BigDecimal.RoundingMode.HALF_EVEN).toDouble
  }

  implicit val floatTypeFormat: FloatJsonFormat = new FloatJsonFormat
  implicit val doubleTypeFormat: DoubleJsonFormat = new DoubleJsonFormat

  implicit val schemaDiffResultFormat: RootJsonFormat[SchemaDiffResult] = jsonFormat6(SchemaDiffResult)
  implicit val createdResponseFormat: RootJsonFormat[CreatedResponse] = jsonFormat3(CreatedResponse.apply)
  implicit val badRequestResponseFormat: RootJsonFormat[BadRequestResponse] = jsonFormat3(BadRequestResponse.apply)
  implicit val internalServerErrorResponseFormat: RootJsonFormat[InternalServerErrorResponse] = jsonFormat3(InternalServerErrorResponse.apply)
  implicit val notFoundResponseFormat: RootJsonFormat[NotFoundResponse] = jsonFormat3(NotFoundResponse.apply)
  implicit val mongoExceptionResponseFormat: RootJsonFormat[MongoExceptionResponse] = jsonFormat3(MongoExceptionResponse.apply)
  implicit val schemaFormat: RootJsonFormat[Schema] = jsonFormat6(Schema.apply)
  implicit val consumerRequestFormat: RootJsonFormat[ConsumerRequest] = jsonFormat4(ConsumerRequest)
  implicit val consumerFormat: RootJsonFormat[Consumer] = jsonFormat5(Consumer.apply)
}
