package com.sumup.unit.dto

import com.sumup.dto.{Consumer, ShortObjectId}
import com.sumup.dto.fields.IntField
import org.scalatest.{Outcome, fixture}

class ConsumerSpec extends fixture.FunSpec {
  type FixtureParam = Consumer
  val name = "test-consumer"
  val schemaName = "test-schema"
  val schemaMajorVersion = 2
  val schemaMinorVersion = 10

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = Consumer(name, schemaName, schemaMajorVersion, schemaMinorVersion)
    test(fixture)
  }

  describe(".apply") {
    it("has `_id`") { fixture => assert(fixture._id.get.isInstanceOf[ShortObjectId]) }
    it("has `name`") { fixture => assert(fixture.name == name) }
    it("has `schemaName`") { fixture => assert(fixture.schemaName == schemaName) }
    it("has `schemaMajorVersion`") { fixture => assert(fixture.schemaMajorVersion == schemaMajorVersion) }
    it("has `schemaMinorVersion`") { fixture => assert(fixture.schemaMinorVersion == schemaMinorVersion) }
  }
}
