package com.sumup.actors.schemas

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import com.sumup.actors.ResponseHandling
import com.sumup.actors.schemas.messages.GetSchemas
import com.sumup.dto.Schema
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.SchemaRepository

object SchemasActor {
  def props()(implicit serializationService: SerializationService, schemaRepository: SchemaRepository): Props = {
    Props(new SchemasActor())
  }
}

class SchemasActor(
                    implicit val serializationService: SerializationService,
                    schemaRepository: SchemaRepository
                  ) extends Actor with ActorLogging with ResponseHandling {
  override def receive: Receive = {
    case GetSchemas(completeFn) =>
      handleInternalServerError(completeFn) {
        var schemas = schemaRepository.getAll

        if (schemas == null) {
          schemas = List[Schema]()
        }

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
}
