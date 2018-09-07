package com.sumup.unit.dto.responses.standard

import akka.http.scaladsl.model.StatusCodes
import com.sumup.dto.responses.custom.SameSchemaResponse
import com.sumup.dto.responses.standard.CreatedResponse
import org.scalatest.{FunSuite, fixture}

class CreatedResponseSpec extends fixture.FunSpec {
  override type FixtureParam = CreatedResponse
  private val statusCode = StatusCodes.Created

  override protected def withFixture(test: OneArgTest) = {
    val obj = CreatedResponse()
    test(obj)
  }

  describe(".apply") {
    it("has `code`") { obj => assert(obj.code == statusCode.intValue)}
    it("has `type`") { obj => assert(obj.`type` == statusCode.reason)}
    it("has `message`") { obj => assert(obj.message == statusCode.defaultMessage)}
  }
}