package com.sumup.storage.codecs

import com.sumup.dto.Schema
import org.bson.Transformer
import org.bson.assertions.Assertions.notNull
import org.bson.codecs.Codec
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import org.mongodb.scala.bson.codecs.BsonTypeClassMap

class SchemaCodecProvider(bsonTypeClassMapArg: BsonTypeClassMap, val valueTransformer: Transformer) extends CodecProvider {
  private val bsonTypeClassMap: BsonTypeClassMap = notNull("bsonTypeClassMap", bsonTypeClassMapArg)

  def this(valueTransformer: Transformer) = this(new BsonTypeClassMap(), valueTransformer)
  def this(bsonTypeClassMap: BsonTypeClassMap) = this(bsonTypeClassMap, null) // scalastyle:off null
  def this() =
    this(new BsonTypeClassMap())

  override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] = {
    if (classOf[Schema].isAssignableFrom(clazz)) {
      new SchemaCodec(registry, bsonTypeClassMap, valueTransformer).asInstanceOf[Codec[T]]
    } else {
      null // scalastyle:off null
    }
  }
}
