package com.sumup.diff.exceptions

case class MajorSchemaChangesException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)
    with DiffException
