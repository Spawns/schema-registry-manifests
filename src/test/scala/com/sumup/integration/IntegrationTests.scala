package com.sumup.integration

import com.sumup.integration.http.routes.{CompatibilityHttpRouteSpec, ConsumerHttpRouteSpec, SchemaHttpRouteSpec, SchemasHttpRouteSpec}
import com.sumup.testutils.ServerSpec
import org.scalatest.{BeforeAndAfterAll, Suites}

class IntegrationTests
  extends Suites(
    new CompatibilityHttpRouteSpec,
    new SchemasHttpRouteSpec,
    new SchemaHttpRouteSpec,
    new ConsumerHttpRouteSpec
  )
  with BeforeAndAfterAll
  with ServerSpec {
  override def beforeAll(): Unit = {
    startServer()
    waitServerBind()
    super.beforeAll()
  }

  override def afterAll: Unit = {
    stopServer()
    super.afterAll()
  }
}
