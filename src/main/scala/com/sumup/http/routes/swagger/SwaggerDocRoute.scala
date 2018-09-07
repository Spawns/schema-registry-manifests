package com.sumup.http.routes.swagger

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import com.sumup.Configuration
import com.sumup.http.routes.{CompatibilityHttpRoute, ConsumerHttpRoute, SchemaHttpRoute, SchemasHttpRoute}
import com.typesafe.config.ConfigFactory

object SwaggerDocRoute extends SwaggerHttpService with Configuration {
  private val address = config.getString("application.http.address")
  private val port = config.getInt("application.http.port")

  override val apiClasses = Set(
    classOf[SchemaHttpRoute],
    classOf[SchemasHttpRoute],
    classOf[CompatibilityHttpRoute],
    classOf[ConsumerHttpRoute]
  )
  override val host = s"$address:$port"
  override val basePath = "/api"
  override val info = Info(version = "1.0")
}
