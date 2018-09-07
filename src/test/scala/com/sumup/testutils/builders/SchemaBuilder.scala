package com.sumup.testutils.builders

import com.sumup.dto.Schema
import com.sumup.dto.fields.{Field, IntField}

object SchemaBuilder {
  final val DEFAULT_NAME = "example-schema"
  final val DEFAULT_APPLICATION_ID = "example-app"
  final var DEFAULT_MAJOR_VERSION = 1
  final var DEFAULT_MINOR_VERSION = 10
  final var DEFAULT_FIELD = IntField("example-int-field")
  def aSchema() = new SchemaBuilder
}

class SchemaBuilder extends Builder[Schema] {
  private var name: String = SchemaBuilder.DEFAULT_NAME
  private var applicationId: String = SchemaBuilder.DEFAULT_APPLICATION_ID
  private var majorVersion: Int = SchemaBuilder.DEFAULT_MAJOR_VERSION
  private var minorVersion: Int = SchemaBuilder.DEFAULT_MINOR_VERSION
  private var fields: List[Field] = List(SchemaBuilder.DEFAULT_FIELD)

  def withName(newName: String): SchemaBuilder = {
    name = newName
    this
  }

  def withApplicationId(newApplicationId: String): SchemaBuilder = {
    applicationId = newApplicationId
    this
  }

  def withMajorVersion(newMajorVersion: Int): SchemaBuilder = {
    majorVersion = newMajorVersion
    this
  }

  def withMinorVersion(newMinorVersion: Int): SchemaBuilder = {
    minorVersion = newMinorVersion
    this
  }

  def withFields(newFields: List[Field]): SchemaBuilder = {
    fields = newFields
    this
  }

  override def build(): Schema = Schema(name, applicationId, majorVersion, minorVersion, fields)
}
