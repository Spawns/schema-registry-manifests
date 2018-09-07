package com.sumup.dto.responses.custom

import akka.http.scaladsl.model.StatusCodes
import com.sumup.dto.responses.StatusResponse
import org.mongodb.scala.{MongoServerException, MongoWriteException}

object MongoExceptionResponse {
  def apply(exception: MongoServerException): MongoExceptionResponse = {
    exception match  {
      case writeException: MongoWriteException =>
        MongoExceptionResponse(
          StatusCodes.BadRequest.intValue,
          "MongoWriteException",
          writeException.getMessage
        )
    }
  }
}

case class MongoExceptionResponse(code: Int, `type`: String, message: String) extends StatusResponse
