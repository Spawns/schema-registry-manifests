package com.sumup.http.routes.swagger

import akka.http.scaladsl.server.{Directives, Route}

trait SwaggerUiRoute extends Directives {
  def swaggerUiRoute: Route =
    path("swagger") {
      getFromResource("swagger/index.html")
    } ~ getFromResourceDirectory("swagger")
}
