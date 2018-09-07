package com.sumup.testutils.builders

import com.sumup.dto.requests.ConsumerRequest


object ConsumerRequestBuilder {
  final val DEFAULT_NAME = "example-consumer-request"
  final val DEFAULT_SCHEMA_NAME = "example-schema"
  final var DEFAULT_SCHEMA_MAJOR_VERSION = 1
  final var DEFAULT_SCHEMA_MINOR_VERSION = 10
  def aConsumerRequest() = new ConsumerRequestBuilder
}

class ConsumerRequestBuilder extends Builder[ConsumerRequest] {
  private var name: String = ConsumerRequestBuilder.DEFAULT_NAME
  private var schemaName: String = ConsumerRequestBuilder.DEFAULT_SCHEMA_NAME
  private var schemaMajorVersion: Int = ConsumerRequestBuilder.DEFAULT_SCHEMA_MAJOR_VERSION
  private var schemaMinorVersion: Int = ConsumerRequestBuilder.DEFAULT_SCHEMA_MINOR_VERSION

  def withName(newName: String): ConsumerRequestBuilder = {
    name = newName
    this
  }

  def withSchemaName(newName: String): ConsumerRequestBuilder = {
    schemaName = schemaName
    this
  }

  def withSchemaMajorVersion(newSchemaMajorVersion: Int): ConsumerRequestBuilder = {
    schemaMajorVersion = newSchemaMajorVersion
    this
  }

  def withSchemaMinorVersion(newSchemaMinorVersion: Int): ConsumerRequestBuilder = {
    schemaMinorVersion = newSchemaMinorVersion
    this
  }

  override def build(): ConsumerRequest = ConsumerRequest(name, schemaName, schemaMajorVersion, schemaMinorVersion)
}

