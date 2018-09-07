package com.sumup.serde

import com.sumup.UtilsService
import com.sumup.diff.entities.SchemaDiffResult
import com.sumup.dto.fields.Field
import com.sumup.dto.requests.{ConsumerRequest, SchemaRequest}
import com.sumup.dto.responses.custom.MongoExceptionResponse
import com.sumup.dto.responses.standard.{BadRequestResponse, CreatedResponse, InternalServerErrorResponse, NotFoundResponse}
import com.sumup.dto.{Consumer, Schema}

class SerializationService(implicit val utilsService: UtilsService) {
  import com.sumup.json.SchemaRegistryJsonProtocol._
  import gnieh.diffson.sprayJson.DiffsonProtocol.OperationFormat
  import gnieh.diffson.sprayJson._
  import spray.json._

  import scala.reflect.runtime.universe._

  def serialize[T: TypeTag](payload: T): String = {
    payload match {
      case consumer: Consumer => consumerFormat.write(consumer).prettyPrint
      case schema: Schema => schemaFormat.write(schema).prettyPrint
      case consumerRequest: ConsumerRequest => consumerRequestFormat.write(consumerRequest).prettyPrint
      case schemaRequest: SchemaRequest => SchemaRequestFormat.write(schemaRequest).prettyPrint
      case diffResult: SchemaDiffResult => schemaDiffResultFormat.write(diffResult).prettyPrint
      case jsValue: JsValue => jsValue.convertTo[String]
      case notFoundResponse: NotFoundResponse => notFoundResponseFormat.write(notFoundResponse).prettyPrint
      case mongoExceptionResponse: MongoExceptionResponse => mongoExceptionResponseFormat.write(mongoExceptionResponse).prettyPrint
      case badRequestResponse: BadRequestResponse => badRequestResponseFormat.write(badRequestResponse).prettyPrint
      case internalServerErrorResponse: InternalServerErrorResponse => internalServerErrorResponseFormat.write(internalServerErrorResponse).prettyPrint
      // NOTE: No need to check the type of the sequence
      // since all that matters is that it's a sequence.
      // More information about the type of the sequence will be extracted via a type tag.
      case _: Seq[T @unchecked] =>
        typeOf[T] match {
          case t if t =:= typeOf[Seq[Schema]] => payload.asInstanceOf[Seq[Schema]].toJson.prettyPrint
          case t if t <:< typeOf[Seq[Field]] => payload.asInstanceOf[Seq[Field]].toJson.prettyPrint
          case t if t =:= typeOf[Seq[Consumer]] => payload.asInstanceOf[Seq[Consumer]].toJson.prettyPrint
        }
    }
  }

  def serializeFieldsSorted(payload: Seq[Field]): String = {
    serialize(utilsService.sortFields(payload))
  }

  def serializeIsCreated(isCreated: Boolean): String = {
    if (isCreated) {
      CreatedResponse().toJson.prettyPrint
    } else {
      BadRequestResponse().toJson.prettyPrint
    }
  }

  def serializeOperation(operation: Operation): JsValue = {
    OperationFormat.write(operation)
  }
}
