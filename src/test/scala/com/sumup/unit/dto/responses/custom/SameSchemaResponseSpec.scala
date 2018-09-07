package com.sumup.unit.dto.responses.custom

import akka.http.scaladsl.model.StatusCodes
import com.sumup.dto.responses.custom.SameSchemaResponse
import org.scalatest.fixture

class SameSchemaResponseSpec extends fixture.FunSpec {
  override type FixtureParam = SameSchemaResponse
  private val statusCode = StatusCodes.OK

  override protected def withFixture(test: OneArgTest) = {
    val obj = SameSchemaResponse()
    test(obj)
  }

  describe(".apply") {
    it("has `code`") { obj => assert(obj.code == statusCode.intValue)}
    it("has `type`") { obj => assert(obj.`type` == statusCode.reason)}
    it("has `message`") { obj => assert(obj.message == "Schemas are compatible")}
  }
}
