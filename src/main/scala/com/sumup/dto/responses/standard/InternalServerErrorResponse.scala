package com.sumup.dto.responses.standard

import com.sumup.dto.responses.StatusResponse
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

object InternalServerErrorResponse {
  final val Code = 500
  final val Reason = "Internal Server Error"
  final val Message = "There was an internal server error."

  def apply(message: String = Message): InternalServerErrorResponse = {
    InternalServerErrorResponse(Code, Reason, message)
  }
}

@ApiModel(description = "Standard response for internal server error", parent = classOf[StatusResponse])
case class InternalServerErrorResponse(
                             @(ApiModelProperty @field)(value = "status code", example = "500")
                             code: Int,
                             @(ApiModelProperty @field)(value = "type code", example = InternalServerErrorResponse.Reason)
                             `type`: String,
                             @(ApiModelProperty @field)(value = "human-readable message", example = InternalServerErrorResponse.Message)
                             message: String
                           ) extends StatusResponse
