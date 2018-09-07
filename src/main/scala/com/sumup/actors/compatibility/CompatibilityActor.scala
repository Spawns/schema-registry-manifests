package com.sumup.actors.compatibility

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import com.sumup.actors.ResponseHandling
import com.sumup.actors.compatibility.messages.CompatibilityCheck
import com.sumup.diff.DiffProcessingService
import com.sumup.diff.exceptions.DiffException
import com.sumup.dto.Schema
import com.sumup.dto.requests.SchemaRequest
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.SchemaRepository

object CompatibilityActor {
  def props()(
    implicit serializationService: SerializationService,
    schemaRepository: SchemaRepository,
    diffProcessingService: DiffProcessingService
  ): Props = {
    Props(new CompatibilityActor())
  }
}

class CompatibilityActor(
                          implicit val serializationService: SerializationService,
                          implicit val schemaRepository: SchemaRepository,
                          implicit val diffProcessingService: DiffProcessingService,
                        ) extends Actor with ActorLogging with ResponseHandling {
  override def receive: Receive = {
    case CompatibilityCheck(completeFn, request) =>
      handleInternalServerError(completeFn) {
        val exactMatchSchemaToProcess = schemaRepository.getByNameAndVersion(
          request.name,
          request.majorVersion,
          request.minorVersion
        )

        if (exactMatchSchemaToProcess == null) {
          val priorMinorSchemaToProcess = schemaRepository.getByNameAndVersion(
            request.name,
            request.majorVersion,
            request.minorVersion - 1
          )

          if (priorMinorSchemaToProcess == null) {
            val priorMajorVersion = request.majorVersion - 1
            val priorMajorSchemaToProcess = schemaRepository.getByNameAndVersion(
              request.name,
              priorMajorVersion,
              schemaRepository.getLastMinorVersion(request.name, priorMajorVersion)
            )

            if (priorMajorSchemaToProcess == null) {
              completeNotFoundResponse(completeFn)
            } else {
              completeCompatibilityRequest(
                completeFn,
                request,
                priorMajorSchemaToProcess,
                isMajorUpgrade = true,
                isMinorUpgrade = false
              )
            }
          } else {
            completeCompatibilityRequest(
              completeFn,
              request,
              priorMinorSchemaToProcess,
              isMajorUpgrade = false,
              isMinorUpgrade = true
            )
          }
        } else {
          completeCompatibilityRequest(
            completeFn,
            request,
            exactMatchSchemaToProcess,
            isMajorUpgrade = false,
            isMinorUpgrade = false
          )
        }
      }
  }

  def completeCompatibilityRequest(
                                    completeFn: HttpResponse => Unit,
                                    request: SchemaRequest,
                                    schema: Schema,
                                    isMajorUpgrade: Boolean,
                                    isMinorUpgrade: Boolean
                                  ): Unit = {
    try {
      val result = diffProcessingService.fullProcess(
        request,
        schema,
        isMajorUpgrade,
        isMinorUpgrade
      )

      completeFn(
        HttpResponse(
          status = StatusCodes.OK,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            serializationService.serialize(result)
          )
        )
      )
    } catch {
      case e: DiffException =>
        completeBadRequest(completeFn, e.getMessage)
    }
  }
}

