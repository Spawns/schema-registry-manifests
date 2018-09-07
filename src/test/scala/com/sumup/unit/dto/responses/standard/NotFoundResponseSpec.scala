package com.sumup.unit.dto.responses.standard

import akka.http.scaladsl.model.StatusCodes
import com.sumup.dto.responses.standard.NotFoundResponse
import org.scalatest.fixture

class NotFoundResponseSpec extends fixture.FunSpec {
  override type FixtureParam = NotFoundResponse
  val statusCode = StatusCodes.NotFound

  override protected def withFixture(test: OneArgTest) = {
    val obj = NotFoundResponse()
    test(obj)
  }

  describe(".apply") {
    it("has `code`") { obj => assert(obj.code == statusCode.intValue)}
    it("has `type`") { obj => assert(obj.`type` == statusCode.reason)}
    it("has `message`") { obj => assert(obj.message == statusCode.defaultMessage)}
  }
}
