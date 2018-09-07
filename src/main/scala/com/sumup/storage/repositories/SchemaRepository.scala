package com.sumup.storage.repositories

import com.sumup.dto.Schema
import com.sumup.dto.fields.Field
import com.sumup.storage.MongoClientWrapper
import com.sumup.storage.repositories.exceptions.GenericException
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Sorts._

class SchemaRepository(implicit val mongoClientWrapper: MongoClientWrapper) extends Repository {

  private val collection = mongoClientWrapper.getDatabase.getCollection[Schema]("schemas")

  def getByName(name: String): Seq[Schema] = {
    collection
      .find(
        equal("name", name)
      )
      .results()
  }

  def getByNameAndVersion(name: String, majorVersion: Int, minorVersion: Int): Schema = {
    collection
      .find(
        and(
          equal("name", name),
          equal("majorVersion", majorVersion),
          equal("minorVersion", minorVersion)
        )
      )
      .first()
      .headResult()
  }

  def getAll: Seq[Schema] = collection.find().results()

  def create(name: String, applicationId: String, majorVersion: Int, minorVersion: Int, fields: List[Field]): Boolean = {
    collection
      .insertOne(
        Schema(name, applicationId, majorVersion, minorVersion, fields)
      )
      .wasSuccess()
  }

  def getLastMajorVersion(name: String): Int = {
    val result = collection
      .find(equal("name", name))
      .sort(descending("name", "majorVersion"))
      .limit(1)
      .first()
      .headResult()

    if (result != null) {
      result.majorVersion
    } else {
      0
    }
  }

  def getLastMinorVersion(name: String, majorVersion: Int): Int = {
    val result = collection
      .find(
        and(
          equal("name", name),
          equal("majorVersion", majorVersion)
        )
      )
      .sort(descending("name", "minorVersion"))
      .limit(1)
      .first()
      .headResult()

    if (result != null) {
      result.minorVersion
    } else {
      0
    }
  }

  def deleteSchemaByNameAndVersion(name: String, majorVersion: Int, minorVersion: Int): Schema = {
    val criteria =
      and(
        equal("name", name),
        equal("majorVersion", majorVersion),
        equal("minorVersion", minorVersion)
      )

    val schema = collection.find(criteria).first().headResult()

    if (schema == null) {
      null
    } else {
      val result = collection.deleteOne(criteria).headResult()

      if (!result.wasAcknowledged() || result.getDeletedCount < 1) {
        throw GenericException(s"Could not delete schema: ${schema.name}-${schema.majorVersion}-${schema.minorVersion}")
      } else {
        schema
      }
    }
  }
}
