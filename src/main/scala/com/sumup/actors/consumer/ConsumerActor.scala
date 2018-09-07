package com.sumup.actors.consumer

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import com.sumup.actors.ResponseHandling
import com.sumup.actors.consumer.messages.{GetConsumer, GetConsumerBySchema, RegisterConsumer}
import com.sumup.dto.responses.standard.NotFoundResponse
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.{ConsumerRepository, SchemaRepository}

object ConsumerActor {
  def props()(
    implicit serializationService: SerializationService,
    schemaRepository: SchemaRepository,
    consumerRepository: ConsumerRepository
  ): Props = {
    Props(new ConsumerActor())
  }
}

class ConsumerActor(
                     implicit val serializationService: SerializationService,
                     consumerRepository: ConsumerRepository,
                     schemaRepository: SchemaRepository
                   ) extends Actor with ActorLogging with ResponseHandling {
  override def receive: Receive = {
    case GetConsumer(completeFn, name) =>
      handleInternalServerError(completeFn) {
        val consumers = consumerRepository.getByName(name)
        if (consumers.isEmpty) {
          completeNotFoundResponse(completeFn)
        } else {
          completeFn(
            HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                serializationService.serialize(consumers)
              )
            )
          )
        }
      }

    case GetConsumerBySchema(completeFn, name, schemaName, schemaMajorVersion, schemaMinorVersion) =>
      handleInternalServerError(completeFn) {
        val consumer = consumerRepository.getByNameAndSchema(
          name,
          schemaName,
          schemaMajorVersion,
          schemaMinorVersion
        )

        if (consumer == null) {
          completeFn(
            HttpResponse(
              status = StatusCodes.NotFound,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                serializationService.serialize(NotFoundResponse())
              )
            )
          )
        } else {
          completeFn(
            HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                serializationService.serialize(consumer)
              )
            )
          )
        }
      }

    case RegisterConsumer(completeFn, request) =>
      handleInternalServerError(completeFn) {
        val consumer = consumerRepository.getByNameAndSchema(request.name, request.schemaName, request.schemaMajorVersion, request.schemaMinorVersion)

        if (consumer != null) {
          completeBadRequest(completeFn, "Consumer already exists.")
        } else {
          val schema = schemaRepository.getByNameAndVersion(request.schemaName, request.schemaMajorVersion, request.schemaMinorVersion)

          if (schema == null) {
            completeBadRequest(completeFn, "Schema specified in consumer request body does not exist.")
          } else {
            val isCreated = consumerRepository.create(
              request.name,
              request.schemaName,
              request.schemaMajorVersion,
              request.schemaMinorVersion
            )
            completeCreatedResponse(completeFn, isCreated)
          }
        }
      }
  }
}

