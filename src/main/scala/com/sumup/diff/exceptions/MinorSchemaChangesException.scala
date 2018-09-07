package com.sumup.diff.exceptions

case class MinorSchemaChangesException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)
    with DiffException
