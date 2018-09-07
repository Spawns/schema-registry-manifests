package com.sumup.storage

import com.sumup.dto.Consumer
import com.sumup.storage.codecs.{SchemaCodecProvider, ShortObjectIdCodec}
import com.typesafe.config.Config
import org.bson.codecs.configuration.{CodecRegistries, CodecRegistry}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.{MongoClient, MongoDatabase}

class MongoClientWrapper(implicit val config: Config) {
  private val client = getClient

  // NOTE: Order is super important.
  // https://jira.mongodb.org/browse/SCALA-338
  private val codecRegistry: CodecRegistry = CodecRegistries.fromRegistries(
    CodecRegistries.fromProviders(
      new SchemaCodecProvider(),
      classOf[Consumer]
    ),
    CodecRegistries.fromCodecs(new ShortObjectIdCodec),
    DEFAULT_CODEC_REGISTRY
  )

  private def getClient: MongoClient = {
    MongoClient(config.getString("application.storage.connection-string"))
  }

  def getDatabase: MongoDatabase = {
    client.getDatabase(config.getString("application.storage.database")).withCodecRegistry(codecRegistry)
  }
}
