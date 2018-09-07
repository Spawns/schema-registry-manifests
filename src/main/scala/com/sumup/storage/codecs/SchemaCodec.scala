package com.sumup.storage.codecs

import com.sumup.dto.fields._
import com.sumup.dto.{FieldType, Schema, ShortObjectId}
import org.bson._
import org.bson.codecs._
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.Decimal128
import org.mongodb.scala.bson.codecs.BsonTypeClassMap
import spray.json.deserializationError

import scala.collection.mutable

object SchemaCodec {
  def apply(registry: CodecRegistry, bsonTypeClassMap: BsonTypeClassMap): SchemaCodec = apply(registry, bsonTypeClassMap, None)
  def apply(registry: CodecRegistry, bsonTypeClassMap: BsonTypeClassMap, valueTransformer: Option[Transformer]): SchemaCodec = {
    new SchemaCodec(registry, bsonTypeClassMap, valueTransformer.getOrElse(DEFAULT_TRANSFORMER))
  }

  private val DEFAULT_TRANSFORMER = new Transformer() {
    def transform(objectToTransform: Object): Object = objectToTransform
  }
}

class SchemaCodec(registry: CodecRegistry, bsonTypeClassMap: BsonTypeClassMap, valueTransformer: Transformer) extends Codec[Schema] {
  lazy val bsonTypeCodecMap = new BsonTypeCodecMap(bsonTypeClassMap, registry)

  override def getEncoderClass: Class[Schema] = classOf[Schema]

  override def encode(writer: BsonWriter, value: Schema, encoderContext: EncoderContext): Unit = {
    writer.writeStartDocument()

    value._id match {
      case Some(shortObjectId) =>
        writer.writeObjectId("_id", shortObjectId.toObjectId)
      // NOTE: Nothing to write
      case None =>
    }

    writer.writeString("name", value.name)
    writer.writeString("applicationId", value.applicationId)
    writer.writeInt32("majorVersion", value.majorVersion)
    writer.writeInt32("minorVersion", value.minorVersion)

    writeFields(value, writer, encoderContext)

    writer.writeEndDocument()
  }

  private def writeFields(value: CompositeFieldLike, writer: BsonWriter, encoderContext: EncoderContext): Unit = {
    writer.writeStartArray("fields")

    value.fields.foreach {
      case field: PrimitiveField =>
        writePrimitiveField(field, writer, encoderContext)
      case arrayField: ArrayField =>
        writeArrayField(arrayField, writer, encoderContext)
      case recordField: RecordField =>
        writeRecordField(recordField, writer, encoderContext)
      case enumField: EnumField[_] =>
        writeEnumField(enumField, writer, encoderContext)
    }

    writer.writeEndArray()
  }

  private def writePrimitiveField(value: Field, writer: BsonWriter, encoderContext: EncoderContext): Unit = {
    writer.writeStartDocument()
    writer.writeString("name", value.name)
    writer.writeString("type", value.`type`.toString)
    writer.writeBoolean("isIdentity", value.isIdentity)
    writer.writeEndDocument()
  }

  private def writeArrayField(value: ArrayField, writer: BsonWriter, encoderContext: EncoderContext): Unit = {
    writer.writeStartDocument()
    writer.writeString("name", value.name)
    writer.writeString("type", value.`type`.toString)
    writer.writeBoolean("isIdentity", value.isIdentity)
    writer.writeString("items", value.items.toString)
    writer.writeEndDocument()
  }

  private def writeRecordField(value: RecordField, writer: BsonWriter, encoderContext: EncoderContext): Unit = {
    writer.writeStartDocument()
    writer.writeString("name", value.name)
    writer.writeString("type", value.`type`.toString)
    writer.writeBoolean("isIdentity", value.isIdentity)
    writeFields(value, writer, encoderContext)
    writer.writeEndDocument()
  }

  private def writeEnumField[A](value: EnumField[A], writer: BsonWriter, encoderContext: EncoderContext): Unit = {
    writer.writeStartDocument()
    writer.writeString("name", value.name)
    writer.writeString("type", value.`type`.toString)
    writer.writeBoolean("isIdentity", value.isIdentity)
    writer.writeString("valueType", value.valueType.toString)

    writer.writeStartArray("allowedValues")

    value.allowedValues.foreach {
      case floatValue: Float => writer.writeDecimal128(Decimal128.parse(floatValue.toString))
      case doubleValue: Double => writer.writeDecimal128(Decimal128.parse(doubleValue.toString))
      case longValue: Long => writer.writeInt64(longValue)
      case intValue: Int => writer.writeInt64(intValue)
      case stringValue: String => writer.writeString(stringValue)
    }

    writer.writeEndArray()

    writer.writeEndDocument()
  }


  override def decode(reader: BsonReader, decoderContext: DecoderContext): Schema = {
    reader.readStartDocument()
    val objectId = reader.readObjectId("_id")
    val name = reader.readString("name")
    val applicationId = reader.readString("applicationId")
    val majorVersion = reader.readInt32("majorVersion")
    val minorVersion = reader.readInt32("minorVersion")

    val fields = readFields(reader, decoderContext)
    reader.readEndDocument()

    val schema = Schema(name, applicationId, majorVersion, minorVersion, fields)
    schema.copy(_id = Some(ShortObjectId.fromObjectId(objectId)))
  }

  private def readFields(reader: BsonReader, decoderContext: DecoderContext): List[Field] = {
    reader.readStartArray()
    val list = mutable.ListBuffer[Field]()

    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      list += readField(reader, decoderContext)
    }

    reader.readEndArray()
    list.toList
  }

  private def readField(reader: BsonReader, decoderContext: DecoderContext): Field = {
    reader.readStartDocument()
    val name = reader.readString("name")
    val fieldType = FieldType.withName(reader.readString("type"))
    val isIdentity = reader.readBoolean("isIdentity")

    fieldType match {
      case FieldType.INT =>
        reader.readEndDocument()
        IntField(name, isIdentity)
      case FieldType.STRING =>
        reader.readEndDocument()
        StringField(name, isIdentity)
      case FieldType.BOOL =>
        reader.readEndDocument()
        BoolField(name, isIdentity)
      case FieldType.BYTES =>
        reader.readEndDocument()
        BytesField(name, isIdentity)
      case FieldType.FLOAT =>
        reader.readEndDocument()
        FloatField(name, isIdentity)
      case FieldType.LONG =>
        reader.readEndDocument()
        LongField(name, isIdentity)
      case FieldType.DOUBLE =>
        reader.readEndDocument()
        DoubleField(name, isIdentity)
      case FieldType.ARRAY =>
        val items = reader.readString("items")
        reader.readEndDocument()
        ArrayField(name, FieldType.withName(items), isIdentity)
      case FieldType.RECORD =>
        val fields = readFields(reader, decoderContext)
        reader.readEndDocument()
        RecordField(name, fields, isIdentity)
      case FieldType.ENUM =>
        val valueTypeString = reader.readString("valueType")

        if (!FieldType.isType(valueTypeString)) { // TODO
          deserializationError(s"`$valueTypeString` is not a known field type")
        }
        val valueType = FieldType.withName(valueTypeString)
        reader.readName("allowedValues")

        reader.readStartArray()

        val field = valueType match {
          case FieldType.STRING => {
            val list = mutable.ListBuffer[String]()
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
              list += reader.readString()
            }

            EnumField(name, list.toList, valueType, isIdentity)
          }
          case FieldType.FLOAT => {
            val list = mutable.ListBuffer[Float]()
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
              list += reader.readDecimal128().toString.toFloat
            }

            EnumField(name, list.toList, valueType, isIdentity)
          }
          case FieldType.DOUBLE => {
            val list = mutable.ListBuffer[Double]()
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
              list += reader.readDecimal128().toString.toDouble
            }

            EnumField(name, list.toList, valueType, isIdentity)
          }
          case FieldType.LONG => {
            val list = mutable.ListBuffer[Long]()
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
              list += reader.readInt64().toString.toLong
            }

            EnumField(name, list.toList, valueType, isIdentity)
          }
          case FieldType.INT => {
            val list = mutable.ListBuffer[Int]()
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
              list += reader.readInt64().asInstanceOf[Int]
            }

            EnumField(name, list.toList, valueType, isIdentity)
          }
        }

        reader.readEndArray()
        reader.readEndDocument()

        field
    }
  }
}
