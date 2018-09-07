package com.sumup.http.routes

import javax.ws.rs.{Path, PathParam}

import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{Directives, Route}
import com.sumup.actors.schema.messages.{CreateSchema, DeleteSchemaVersion, GetSchema, GetSchemaVersion}
import com.sumup.dto.Schema
import com.sumup.dto.requests.SchemaRequest
import com.sumup.dto.responses.standard.{BadRequestResponse, CreatedResponse, NotFoundResponse}
import io.swagger.annotations._

@Api(
  value = "/schema",
  produces = "application/json",
  consumes = "application/json"
)
@Path("/schema")
trait SchemaHttpRoute extends Directives {
  import com.sumup.json.SchemaRegistryJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  implicit val schemaActorPool: ActorRef

  def schemaVersionRoutes(name: String): Route = {
    pathPrefix("major_version") {
      pathPrefix(Segment) { majorVersion =>
        pathPrefix("minor_version") {
          path(Segment) { minorVersion =>
            pathEndOrSingleSlash {
              getSchemaVersionRoute(name, majorVersion, minorVersion) ~
                deleteSchemaVersionRoute(name, majorVersion, minorVersion)
            }
          }
        }
      }
    }
  }

  def schemaRoutes: Route =
    pathPrefix("schema") {
      createSchemaRoute ~
        pathPrefix(Segment) { name =>
          schemaVersionRoutes(name) ~
            getSchemaRoute(name)
        }
    }

  @ApiOperation(
    value = "Create a schema",
    httpMethod = "POST",
    response = classOf[CreatedResponse]
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
      new ApiResponse(code = 201, message = "Created", response = classOf[CreatedResponse]),
      new ApiResponse(code = 400, message = "Bad Request", response = classOf[BadRequestResponse]),
      new ApiResponse(code = 500, message = "Internal server error")
    )
  )
  def createSchemaRoute: Route = {
    pathEndOrSingleSlash {
      post {
        entity(as[SchemaRequest]) { request =>
          completeWith(implicitly[ToResponseMarshaller[HttpResponse]]) { completeFn =>
            schemaActorPool ! CreateSchema(completeFn, request)
          }
        }
      }
    }
  }

  @Path("/{name}")
  @ApiOperation(
    value = "Get a schema by name and all its versions",
    httpMethod = "GET",
    response = classOf[Schema],
    responseContainer = "List"
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 404, message = "Not Found", response = classOf[NotFoundResponse]),
      new ApiResponse(code = 500, message = "Internal server error")
    )
  )
  def getSchemaRoute(
                      @ApiParam(value = "name of Schema", required = true, example = "transactions") @PathParam("name") name: String
                    ): Route = {
    pathEndOrSingleSlash {
      get {
        completeWith(implicitly[ToResponseMarshaller[HttpResponse]]) { completeFn =>
          schemaActorPool ! GetSchema(completeFn, name)
        }
      }
    }
  }

  @Path("/{name}/major_version/{major_version}/minor_version/{minor_version}")
  @ApiOperation(
    value = "Get a schema by name and version",
    httpMethod = "GET",
    response = classOf[Schema]
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 404, message = "Not Found", response = classOf[NotFoundResponse]),
      new ApiResponse(code = 500, message = "Internal server error")
    )
  )
  def getSchemaVersionRoute(
                             @ApiParam(value = "name of Schema", required = true, example = "transactions") @PathParam("name") name: String,
                             @ApiParam(value = "major version of Schema", required = true, example = "2") @PathParam("major_version") majorVersion: String,
                             @ApiParam(value = "minor version of Schema", required = true, example = "9") @PathParam("minor_version") minorVersion: String
                           ): Route =
    get {
      completeWith(implicitly[ToResponseMarshaller[HttpResponse]]) { completeFn =>
        schemaActorPool ! GetSchemaVersion(completeFn, name, majorVersion.toInt, minorVersion.toInt)
      }
    }

  @Path("/{name}/major_version/{major_version}/minor_version/{minor_version}")
  @ApiOperation(
    value = "Delete a schema by name and version",
    httpMethod = "DELETE",
    response = classOf[Schema]
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "OK", response = classOf[Schema]),
      new ApiResponse(code = 404, message = "Not Found", response = classOf[NotFoundResponse]),
      new ApiResponse(code = 500, message = "Internal server error")
    )
  )
  def deleteSchemaVersionRoute(
                                @ApiParam(value = "name of Schema", required = true, example = "transactions") @PathParam("name") name: String,
                                @ApiParam(value = "major version of Schema", required = true, example = "2") @PathParam("major_version") majorVersion: String,
                                @ApiParam(value = "minor version of Schema", required = true, example = "9") @PathParam("minor_version") minorVersion: String
                              ): Route =
    delete {
      completeWith(implicitly[ToResponseMarshaller[HttpResponse]]) { completeFn =>
        schemaActorPool ! DeleteSchemaVersion(completeFn, name, majorVersion.toInt, minorVersion.toInt)
      }
    }
}
