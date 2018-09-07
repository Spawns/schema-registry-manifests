package com.sumup.creation.exceptions

case class SchemaCreationException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)
