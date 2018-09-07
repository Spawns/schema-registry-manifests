package com.sumup.dto.responses.standard

import akka.http.scaladsl.model.StatusCodes
import com.sumup.dto.responses.StatusResponse
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

object BadRequestResponse {
  final val Code =  400
  final val Reason = "Bad Request"
  final val DefaultMessage = "The request contains bad syntax or cannot be fulfilled."

  def apply(message: String = DefaultMessage): BadRequestResponse = {
    BadRequestResponse(Code, Reason, message)
  }
}

@ApiModel(description = "Standard response for bad request", parent = classOf[StatusResponse])
case class BadRequestResponse(
                               @(ApiModelProperty @field)(value = "status code", example = "400")
                               code: Int,
                               @(ApiModelProperty @field)(value = "type code", example = BadRequestResponse.Reason)
                               `type`: String,
                               @(ApiModelProperty @field)
                               (value = "human-readable message", example = BadRequestResponse.DefaultMessage)
                               message: String
                             ) extends StatusResponse
