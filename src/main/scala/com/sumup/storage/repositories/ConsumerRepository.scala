package com.sumup.storage.repositories

import com.sumup.dto.Consumer
import com.sumup.storage.MongoClientWrapper
import org.mongodb.scala.model.Filters._

class ConsumerRepository(implicit val mongoClientWrapper: MongoClientWrapper) extends Repository {
  private val collection = mongoClientWrapper.getDatabase.getCollection[Consumer]("consumers")

  def getByNameAndSchema(name: String, schemaName: String, schemaMajorVersion: Int, schemaMinorVersion: Int): Consumer = {
    collection
      .find(
        and(
          equal("name", name),
          equal("schemaName", schemaName),
          equal("schemaMajorVersion", schemaMajorVersion),
          equal("schemaMinorVersion", schemaMinorVersion)
        )
      )
      .first()
      .headResult()
  }

  def getByName(name: String): Seq[Consumer] = {
    collection
      .find(
        equal("name", name)
      )
      .results()
  }

  def create(name: String, schemaName: String, schemaMajorVersion: Int, schemaMinorVersion: Int): Boolean = {
    collection
      .insertOne(
        Consumer(name, schemaName, schemaMajorVersion, schemaMinorVersion)
      )
      .wasSuccess()
  }
}
