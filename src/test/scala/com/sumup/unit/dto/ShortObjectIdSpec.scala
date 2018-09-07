package com.sumup.unit.dto

import java.time.Instant
import java.util.Date

import com.sumup.dto.ShortObjectId
import org.bson.types.ObjectId
import org.scalatest.{Outcome, fixture}

class ShortObjectIdSpec extends fixture.FunSpec {
  type FixtureParam = ShortObjectId
  val id = 1234
  val timestamp = Instant.now()

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = new ShortObjectId(id, timestamp)
    test(fixture)
  }

  describe("#toObjectId") {
    it("creates an equivalent `ObjectId`") { fixture =>
      val objectId = fixture.toObjectId
      assert(objectId.getCounter == fixture.id)
      assert(objectId.getTimestamp == fixture.timestamp.getEpochSecond)
      assert(objectId.isInstanceOf[ObjectId])
    }
  }

  describe("#apply") {
    it("has `id`") { fixture => assert(fixture.id == id) }
    it("has `timestamp`") { fixture => assert(fixture.timestamp.compareTo(timestamp) == 0) }
  }
}
