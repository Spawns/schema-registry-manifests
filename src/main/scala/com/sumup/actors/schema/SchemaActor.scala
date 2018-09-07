package com.sumup.actors.schema

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import com.sumup.actors.ResponseHandling
import com.sumup.actors.schema.messages.{CreateSchema, DeleteSchemaVersion, GetSchema, GetSchemaVersion}
import com.sumup.creation.SchemaCreationService
import com.sumup.creation.exceptions.SchemaCreationException
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.SchemaRepository

object SchemaActor {
  def props()(
    implicit serializationService: SerializationService,
    schemaRepository: SchemaRepository,
    schemaCreationService: SchemaCreationService
  ): Props = {
    Props(new SchemaActor())
  }
}

class SchemaActor(
                   implicit val serializationService: SerializationService,
                   schemaRepository: SchemaRepository,
                   schemaCreationService: SchemaCreationService
                 ) extends Actor with ActorLogging with ResponseHandling {
  override def receive: Receive = {
    case CreateSchema(completeFn, schemaRequest) =>
      handleInternalServerError(completeFn) {
        try {
          val isCreated = schemaCreationService.createSchema(
            schemaRequest.name,
            schemaRequest.applicationId,
            schemaRequest.majorVersion,
            schemaRequest.minorVersion,
            schemaRequest.fields
          )

          completeCreatedResponse(
            completeFn,
            isCreated
          )
        } catch {
          case e: SchemaCreationException =>
            completeBadRequest(
              completeFn,
              e.getMessage
            )
        }
      }

    case GetSchema(completeFn, name) =>
      handleInternalServerError(completeFn) {
        val schemas = schemaRepository.getByName(name)

        if (schemas.isEmpty) {
          completeNotFoundResponse(completeFn)
        } else {
          completeFn(
            HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                serializationService.serialize(schemas)
              )
            )
          )
        }
      }

    case GetSchemaVersion(completeFn, name, majorVersion, minorVersion) =>
      handleInternalServerError(completeFn) {
        val schema = schemaRepository.getByNameAndVersion(
          name,
          majorVersion,
          minorVersion
        )

        if (schema == null) {
          completeNotFoundResponse(completeFn)
        } else {
          completeFn(
            HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                serializationService.serialize(schema)
              )
            )
          )
        }
      }

    case DeleteSchemaVersion(completeFn, name, majorVersion, minorVersion) =>
      handleInternalServerError(completeFn) {
        val schema = schemaRepository.deleteSchemaByNameAndVersion(
          name,
          majorVersion,
          minorVersion
        )

        if (schema == null) {
          completeNotFoundResponse(completeFn)
        } else {
          completeFn(
            HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                serializationService.serialize(schema)
              )
            )
          )
        }
      }
  }
}

