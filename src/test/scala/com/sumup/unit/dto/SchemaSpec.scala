package com.sumup.unit.dto

import java.time.Instant

import com.sumup.dto.{Schema, ShortObjectId}
import com.sumup.dto.fields.IntField
import org.bson.types.ObjectId
import org.scalatest.{Outcome, fixture}

class SchemaSpec extends fixture.FunSpec {
  type FixtureParam = Schema
  val name = "test-schema"
  val applicationId = "test-app"
  val majorVersion = 2
  val minorVersion = 10
  val field = IntField("test-field")
  val fields = List(field)

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = Schema(name, applicationId, majorVersion, minorVersion, fields)
    test(fixture)
  }

  describe("#apply") {
    it("has `_id`") { fixture => assert(fixture._id.get.isInstanceOf[ShortObjectId]) }
    it("has `applicationId`") { fixture => assert(fixture.applicationId == applicationId) }
    it("has `name`") { fixture => assert(fixture.name == name) }
    it("has `majorVersion`") { fixture => assert(fixture.majorVersion == majorVersion) }
    it("has `minorVersion`") { fixture => assert(fixture.minorVersion == minorVersion) }
    it("has `fields`") { fixture => assert(fixture.fields == fields) }
  }
}
