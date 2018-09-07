package com.sumup.storage.codecs

import com.sumup.dto.ShortObjectId
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

class ShortObjectIdCodec extends Codec[ShortObjectId] {
  override def decode(reader: BsonReader, decoderContext: DecoderContext): ShortObjectId = {
    ShortObjectId.fromObjectId(reader.readObjectId())
  }
  override def encode(writer: BsonWriter, value: ShortObjectId, encoderContext: EncoderContext): Unit = {
    writer.writeObjectId(value.toObjectId)
  }
  override def getEncoderClass: Class[ShortObjectId] = classOf[ShortObjectId]
}
