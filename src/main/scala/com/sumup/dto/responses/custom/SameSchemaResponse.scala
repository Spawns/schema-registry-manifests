package com.sumup.dto.responses.custom

import akka.http.scaladsl.model.StatusCodes
import com.sumup.dto.responses.StatusResponse

object SameSchemaResponse {
  def apply(): SameSchemaResponse = {
    val code = StatusCodes.OK
    SameSchemaResponse(code.intValue, code.reason, "Schemas are compatible")
  }
}

case class SameSchemaResponse(code: Int, `type`: String, message: String) extends StatusResponse
