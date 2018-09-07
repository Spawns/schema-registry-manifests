package com.sumup.http.routes

import javax.ws.rs.Path

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import com.sumup.actors.compatibility.messages.CompatibilityCheck
import com.sumup.diff.entities.SchemaDiffResult
import com.sumup.dto.requests.SchemaRequest
import com.typesafe.config.Config
import io.swagger.annotations._

@Api(
  value = "/compatibility",
  produces = "application/json",
  consumes = "application/json"
)
@Path("/compatibility")
trait CompatibilityHttpRoute extends Directives {
  import com.sumup.json.SchemaRegistryJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  implicit val compatibilityActorPool: ActorRef

  def compatibilityRoutes: Route = {
    path("compatibility") {
      pathEndOrSingleSlash {
        postCompatibilityRoute
      }
    }
  }

  @ApiOperation(
    value = "Verify that the submitted schema is compatible and/or upgradable with the one stored.",
    httpMethod = "POST",
    response = classOf[SchemaDiffResult]
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "body",
        required = true,
        dataTypeClass = classOf[SchemaRequest],
        paramType = "body"
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 400, message = "Bad Request", response = classOf[SchemaDiffResult]),
      new ApiResponse(code = 500, message = "Internal server error")
    )
  )
  def postCompatibilityRoute: Route =
    post {
      entity(as[SchemaRequest]) { request =>
        completeWith(implicitly[ToResponseMarshaller[HttpResponse]]) { completeFn =>
          compatibilityActorPool ! CompatibilityCheck(completeFn, request)
        }
      }
    }
}
