package com.sumup.creation

import com.sumup.creation.exceptions.SchemaCreationException
import com.sumup.dto.fields.Field
import com.sumup.storage.repositories.SchemaRepository

class SchemaCreationService(implicit val schemaRepository: SchemaRepository) {
  def createSchema(name: String, applicationId: String, majorVersion: Int, minorVersion: Int, fields: List[Field]): Boolean = {
    if (majorVersion < 1) {
      throw SchemaCreationException("Schema major versions start from 1")
    }

    if (minorVersion < 1) {
      throw SchemaCreationException("Schema minor versions start from 1")
    }

    if (applicationId == null || applicationId.isEmpty) {
      throw SchemaCreationException("Schema application id is blank/empty")
    }

    val schema = schemaRepository.getByNameAndVersion(name, majorVersion, minorVersion)

    if (schema != null) {
      throw SchemaCreationException("Schema already exists.")
    }

    val lastMajorVersion = schemaRepository.getLastMajorVersion(name)

    if (lastMajorVersion + 1 != majorVersion) {
      throw SchemaCreationException(
        "Schema skips major version(s)." +
          s" Last major version is: $lastMajorVersion." +
          s" New major version must be: ${lastMajorVersion + 1}"
      )
    }

    val lastMinorVersion = schemaRepository.getLastMinorVersion(name, minorVersion)

    if (lastMinorVersion + 1 != minorVersion) {
      throw SchemaCreationException(
        "Schema skips minor version(s)." +
          s" Last minor version is: $lastMinorVersion." +
          s" New minor version must be: ${lastMinorVersion + 1}"
      )
    }

    schemaRepository.create(
      name,
      applicationId,
      majorVersion,
      minorVersion,
      fields
    )
  }
}
