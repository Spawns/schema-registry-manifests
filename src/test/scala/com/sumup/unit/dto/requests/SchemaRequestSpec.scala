package com.sumup.unit.dto.requests

import com.sumup.dto.fields.{Field, IntField}
import com.sumup.dto.requests.SchemaRequest
import org.scalatest.{Outcome, fixture}

class SchemaRequestSpec extends fixture.FunSpec {
  type FixtureParam = SchemaRequest
  val name = "testRequest"
  val applicationId = "test-app"
  val majorVersion = 1
  val minorVersion = 1
  val fields = List[Field](IntField("foo"), IntField("bar"))

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = SchemaRequest(name, applicationId, majorVersion, minorVersion, fields)
    test(fixture)
  }

  describe("#apply") {
    it("has `name`") { fixture => assert(fixture.name == name) }
    it("has `majorVersion`") { fixture => assert(fixture.majorVersion == majorVersion) }
    it("has `minorVersion`") { fixture => assert(fixture.minorVersion == minorVersion) }
    it("has `fields`") { fixture => assert(fixture.fields == fields) }
  }
}
