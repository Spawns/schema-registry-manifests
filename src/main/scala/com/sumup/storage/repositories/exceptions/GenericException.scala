package com.sumup.storage.repositories.exceptions

// TODO: Remove and replace with more concrete exceptions
case class GenericException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)
    with RepositoryException

