package com.sumup.dto.responses.standard

import javax.ws.rs.DefaultValue

import akka.http.scaladsl.model.StatusCodes
import com.sumup.dto.responses.StatusResponse
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

object CreatedResponse {
  final val Code = 201
  final val Reason = "Created"
  final val Message = "The request has been fulfilled and resulted in a new resource being created."

  def apply(): CreatedResponse = {
    CreatedResponse(Code, Reason, Message)
  }
}

@ApiModel(description = "Standard response for created entities", parent = classOf[StatusResponse])
case class CreatedResponse(
                            @(ApiModelProperty @field)(value = "status code", example = "201")
                            code: Int,
                            @(ApiModelProperty @field)(value = "type code", example = "Created")
                            `type`: String,
                            @(ApiModelProperty @field)
                            (
                              value = "human-readable message",
                              example = "The request has been fulfilled and resulted in a new resource being created."
                            )
                            message: String
                          ) extends StatusResponse
