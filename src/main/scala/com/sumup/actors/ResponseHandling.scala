package com.sumup.actors

import akka.actor.ActorLogging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import com.sumup.dto.responses.standard.{BadRequestResponse, InternalServerErrorResponse, NotFoundResponse}
import com.sumup.serde.SerializationService

trait ResponseHandling {
  self: ActorLogging =>

  implicit val serializationService: SerializationService

  def handleInternalServerError(completeFn: (HttpResponse) => Unit)(codeBlock: => Unit): Unit = {
    try {
      codeBlock
    } catch {
      case e: Exception =>
        log.error(e.getMessage)
        completeInternalServerError(completeFn, e)
    }
  }

  def completeCreatedResponse(completeFn: (HttpResponse) => Unit, isCreated: Boolean): Unit = {
    completeFn(
      HttpResponse(
        status = if (isCreated) StatusCodes.Created else StatusCodes.BadRequest,
        entity = HttpEntity(
          ContentTypes.`application/json`,
          serializationService.serializeIsCreated(isCreated)
        )
      )
    )
  }

  def completeBadRequest(completeFn: (HttpResponse) => Unit, message: String = BadRequestResponse.DefaultMessage): Unit = {
    completeFn(
      HttpResponse(
        status = StatusCodes.BadRequest,
        entity = HttpEntity(
          ContentTypes.`application/json`,
          serializationService.serialize(
            BadRequestResponse(message)
          )
        )
      )
    )
  }

  def completeInternalServerError(completeFn: (HttpResponse) => Unit, e: Throwable): Unit = {
    completeFn(
      HttpResponse(
        status = StatusCodes.InternalServerError,
        entity = HttpEntity(
          ContentTypes.`application/json`,
          serializationService.serialize(InternalServerErrorResponse(e.getMessage))
        )
      )
    )
  }

  def completeNotFoundResponse(completeFn: (HttpResponse) => Unit): Unit = {
    completeFn(
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`application/json`,
          serializationService.serialize(NotFoundResponse())
        )
      )
    )
  }
}
