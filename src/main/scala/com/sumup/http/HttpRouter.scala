package com.sumup.http

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{RejectionHandler, Route, RouteConcatenation}
import com.sumup.dto.responses.standard.NotFoundResponse
import com.sumup.http.routes.swagger.{SwaggerDocRoute, SwaggerUiRoute}
import com.sumup.http.routes.{CompatibilityHttpRoute, ConsumerHttpRoute, SchemaHttpRoute, SchemasHttpRoute}
import com.sumup.serde.SerializationService

trait HttpRouter
  extends SchemaHttpRoute
    with SchemasHttpRoute
    with CompatibilityHttpRoute
    with ConsumerHttpRoute
    with SwaggerUiRoute
    with RouteConcatenation {
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors

  implicit val serializationService: SerializationService

  implicit def rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handleNotFound {
        complete(
          HttpResponse(
            StatusCodes.NotFound,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              serializationService.serialize(NotFoundResponse())
            )
          )
        )
      }
      .result()

  def routes: Route = cors() (apiRoutes ~ SwaggerDocRoute.routes ~ swaggerUiRoute)

  private def apiRoutes = {
    pathPrefix("api") {
      schemaRoutes ~
        schemasRoutes ~
        consumerRoutes ~
        compatibilityRoutes
    }
  }
}

