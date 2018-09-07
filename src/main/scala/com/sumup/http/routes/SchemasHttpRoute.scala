package com.sumup.http.routes

import javax.ws.rs.Path

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{Directives, Route}
import com.sumup.actors.schemas.messages.GetSchemas
import com.sumup.dto.Schema
import io.swagger.annotations._

@Api(
  value = "/schemas",
  produces = "application/json",
  consumes = "application/json"
)
@Path("/schemas")
trait SchemasHttpRoute extends Directives {

  implicit val schemasActorPool: ActorRef

  def schemasRoutes: Route = path("schemas") {
    getSchemasRoute
  }

  @ApiOperation(
    value = "Get all schemas",
    httpMethod = "GET",
    response = classOf[Schema],
    responseContainer = "List"
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 500, message = "Internal server error")
    )
  )
  def getSchemasRoute: Route =
    pathEndOrSingleSlash {
      get {
        completeWith(implicitly[ToResponseMarshaller[HttpResponse]]) { completeFn =>
          schemasActorPool ! GetSchemas(completeFn)
        }
      }
    }
}
