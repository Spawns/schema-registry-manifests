package com.sumup.dto.responses.standard

import akka.http.scaladsl.model.StatusCodes
import com.sumup.dto.responses.StatusResponse
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

object NotFoundResponse {
  final val Code = 404
  final val Reason = "Not Found"
  final val Message = "The requested resource could not be found but may be available again in the future."

  def apply(): NotFoundResponse = {
    NotFoundResponse(Code, Reason, Message)
  }
}

@ApiModel(description = "Standard response for not found entities or paths", parent = classOf[StatusResponse])
case class NotFoundResponse(
                             @(ApiModelProperty @field)(value = "status code", example = "404")
                             code: Int,
                             @(ApiModelProperty @field)(value = "type code", example = NotFoundResponse.Reason)
                             `type`: String,
                             @(ApiModelProperty @field)
                             (
                               value = "human-readable message",
                               example = NotFoundResponse.Message
                             )
                             message: String
                           ) extends StatusResponse
