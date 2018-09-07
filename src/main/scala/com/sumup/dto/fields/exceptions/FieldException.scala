package com.sumup.dto.fields.exceptions

case class FieldException(message: String, cause: Throwable = None.orNull)
  extends Exception(message, cause)
