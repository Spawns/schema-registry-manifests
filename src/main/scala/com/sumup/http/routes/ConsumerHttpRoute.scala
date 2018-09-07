package com.sumup.http.routes

import javax.ws.rs.{Path, PathParam}

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{Directives, Route}
import com.sumup.actors.consumer.messages.{GetConsumer, GetConsumerBySchema, RegisterConsumer}
import com.sumup.dto.Consumer
import com.sumup.dto.requests.ConsumerRequest
import com.sumup.dto.responses.standard.{BadRequestResponse, CreatedResponse, NotFoundResponse}
import io.swagger.annotations._

@Api(
  value = "/consumer",
  produces = "application/json",
  consumes = "application/json"
)
@Path("/consumer")
trait ConsumerHttpRoute extends Directives {
  import com.sumup.json.SchemaRegistryJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  implicit val consumerActorPool: ActorRef

  def consumerRoutes: Route = {
    pathPrefix("consumer") {
      registerConsumer() ~
      pathPrefix(Segment) { name =>
        consumerSchemaRoutes(name) ~
        getConsumerRoute(name)
      }
    }
  }

  def consumerSchemaRoutes(name: String): Route = {
    pathPrefix("schema_name") {
      pathPrefix(Segment) { schemaName =>
        pathPrefix("schema_major_version") {
          pathPrefix(Segment) { schemaMajorVersion =>
            pathPrefix("schema_minor_version") {
              path(Segment) { schemaMinorVersion =>
                pathEndOrSingleSlash {
                  getConsumerBySchemaRoute(name, schemaName, schemaMajorVersion, schemaMinorVersion)
                }
              }
            }
          }
        }
      }
    }
  }

  @ApiOperation(
    value = "Register a consumer",
    httpMethod = "POST",
    response = classOf[CreatedResponse]
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "body",
        required = true,
        dataTypeClass = classOf[ConsumerRequest],
        paramType = "body"
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 201, message = "Created", response = classOf[CreatedResponse]),
      new ApiResponse(code = 400, message = "Bad Request", response = classOf[BadRequestResponse]),
      new ApiResponse(code = 500, message = "Internal server error")
    )
  )
  def registerConsumer(): Route = {
    pathEndOrSingleSlash {
      post {
        entity(as[ConsumerRequest]) { request =>
          completeWith(implicitly[ToResponseMarshaller[HttpResponse]]) { completeFn =>
            consumerActorPool ! RegisterConsumer(completeFn, request)
          }
        }
      }
    }
  }

  @Path("/{name}/schema_name/{schemaName}/schema_major_version/{schemaMajorVersion}/schema_minor_version/{schemaMinorVersion}")
  @ApiOperation(
    value = "Get a consumer by name, schema_name, schema_major_version and schema_minor_version",
    httpMethod = "GET",
    response = classOf[Consumer]
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 404, message = "Not Found", response = classOf[NotFoundResponse]),
      new ApiResponse(code = 500, message = "Internal server error")
    )
  )
  def getConsumerBySchemaRoute(
    @ApiParam(value = "name of Consumer", required = true, example = "transactions-app") @PathParam("name") name: String,
    @ApiParam(value = "name of Schema", required = true, example = "11844835") @PathParam("schemaName") schemaName: String,
    @ApiParam(value = "major version of Schema", required = true, example = "1") @PathParam("schemaMajorVersion") schemaMajorVersion: String,
    @ApiParam(value = "minor version of Schema", required = true, example = "2") @PathParam("schemaMinorVersion") schemaMinorVersion: String
  ): Route =
    get {
      completeWith(implicitly[ToResponseMarshaller[HttpResponse]]) { completeFn =>
        consumerActorPool ! GetConsumerBySchema(
          completeFn,
          name,
          schemaName,
          schemaMajorVersion.toInt,
          schemaMinorVersion.toInt
        )
      }
    }

  @Path("/{name}")
  @ApiOperation(
    value = "Get a consumers by name",
    httpMethod = "GET",
    response = classOf[Consumer],
    responseContainer = "List"
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 404, message = "Not Found", response = classOf[NotFoundResponse]),
      new ApiResponse(code = 500, message = "Internal server error")
    )
  )
  def getConsumerRoute(
                        @ApiParam(value = "name of Consumer", required = true, example = "transactions-app") @PathParam("name") name: String
                      ): Route = {
    pathEndOrSingleSlash {
      get {
        completeWith(implicitly[ToResponseMarshaller[HttpResponse]]) { completeFn =>
          consumerActorPool ! GetConsumer(completeFn, name)
        }
      }
    }
  }
}
