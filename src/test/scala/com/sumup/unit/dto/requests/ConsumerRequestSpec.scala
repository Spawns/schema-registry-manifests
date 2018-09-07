package com.sumup.unit.dto.requests

import com.sumup.dto.fields.{Field, IntField}
import com.sumup.dto.requests.ConsumerRequest
import org.scalatest.{Outcome, fixture}

class ConsumerRequestSpec extends fixture.FunSpec {
  type FixtureParam = ConsumerRequest
  val name = "testRequest"
  val schemaName = "testSchema"
  val schemaMajorVersion = 1
  val schemaMinorVersion = 1

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = ConsumerRequest(name, schemaName, schemaMajorVersion, schemaMinorVersion)
    test(fixture)
  }

  describe("#apply") {
    it("has `name`") { fixture => assert(fixture.name == name) }
    it("has `schemaName`") { fixture => assert(fixture.schemaName == schemaName) }
    it("has `schemaMajorVersion`") { fixture => assert(fixture.schemaMajorVersion == schemaMajorVersion) }
    it("has `schemaMinorVersion`") { fixture => assert(fixture.schemaMinorVersion == schemaMinorVersion) }
  }
}
