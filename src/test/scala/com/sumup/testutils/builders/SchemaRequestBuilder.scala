package com.sumup.testutils.builders

import com.sumup.dto.fields.{Field, IntField}
import com.sumup.dto.requests.SchemaRequest

object SchemaRequestBuilder {
  final val DEFAULT_NAME = "example-schema-request"
  final val DEFAULT_APPLICATION_ID = "example-app"
  final var DEFAULT_MAJOR_VERSION = 1
  final var DEFAULT_MINOR_VERSION = 10
  final var DEFAULT_FIELD = IntField("example-int-field")
  def aSchemaRequest() = new SchemaRequestBuilder
}

class SchemaRequestBuilder extends Builder[SchemaRequest] {
  private var name: String = SchemaRequestBuilder.DEFAULT_NAME
  private var applicationId: String = SchemaRequestBuilder.DEFAULT_APPLICATION_ID
  private var majorVersion: Int = SchemaRequestBuilder.DEFAULT_MAJOR_VERSION
  private var minorVersion: Int = SchemaRequestBuilder.DEFAULT_MINOR_VERSION
  private var fields: List[Field] = List(SchemaRequestBuilder.DEFAULT_FIELD)

  def withName(newName: String): SchemaRequestBuilder = {
    name = newName
    this
  }

  def withApplicationId(newApplicationId: String): SchemaRequestBuilder = {
    applicationId = newApplicationId
    this
  }

  def withMajorVersion(newMajorVersion: Int): SchemaRequestBuilder = {
    majorVersion = newMajorVersion
    this
  }

  def withMinorVersion(newMinorVersion: Int): SchemaRequestBuilder = {
    minorVersion = newMinorVersion
    this
  }

  def withFields(newFields: List[Field]): SchemaRequestBuilder = {
    fields = newFields
    this
  }

  override def build(): SchemaRequest = SchemaRequest(name, applicationId, majorVersion, minorVersion, fields)
}
