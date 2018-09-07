package com.sumup.testutils.builders

import com.sumup.dto.Consumer

object ConsumerBuilder {
  final val DEFAULT_NAME = "example-consumer-request"
  final val DEFAULT_SCHEMA_NAME = "example-schema"
  final var DEFAULT_SCHEMA_MAJOR_VERSION = 1
  final var DEFAULT_SCHEMA_MINOR_VERSION = 10
  def aConsumer() = new ConsumerBuilder
}

class ConsumerBuilder extends Builder[Consumer] {
  private var name: String = ConsumerBuilder.DEFAULT_NAME
  private var schemaName: String = ConsumerBuilder.DEFAULT_SCHEMA_NAME
  private var schemaMajorVersion: Int = ConsumerBuilder.DEFAULT_SCHEMA_MAJOR_VERSION
  private var schemaMinorVersion: Int = ConsumerBuilder.DEFAULT_SCHEMA_MINOR_VERSION

  def withName(newName: String): ConsumerBuilder = {
    name = newName
    this
  }

  def withSchemaName(newName: String): ConsumerBuilder = {
    schemaName = schemaName
    this
  }

  def withSchemaMajorVersion(newSchemaMajorVersion: Int): ConsumerBuilder = {
    schemaMajorVersion = newSchemaMajorVersion
    this
  }

  def withSchemaMinorVersion(newSchemaMinorVersion: Int): ConsumerBuilder = {
    schemaMinorVersion = newSchemaMinorVersion
    this
  }

  override def build(): Consumer = Consumer(name, schemaName, schemaMajorVersion, schemaMinorVersion)
}

