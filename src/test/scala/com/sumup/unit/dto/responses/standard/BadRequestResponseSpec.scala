package com.sumup.unit.dto.responses.standard

import akka.http.scaladsl.model.StatusCodes
import com.sumup.dto.responses.standard.BadRequestResponse
import org.scalatest.fixture

class BadRequestResponseSpec extends fixture.FunSpec {
  override type FixtureParam = BadRequestResponse
  val statusCode = StatusCodes.BadRequest

  override protected def withFixture(test: OneArgTest) = {
    val obj = BadRequestResponse()
    test(obj)
  }

  describe(".Code") {
    it("is `400`") { _ => assert(BadRequestResponse.Code == 400) }
  }

  describe (".Reason") {
    it("is `BadRequest`") { _ => assert(BadRequestResponse.Reason == "Bad Request") }
  }

  describe (".DefaultMessage") {
    it("is `The request contains bad syntax or cannot be fulfilled.`") {
      _ =>
        assert(BadRequestResponse.DefaultMessage == "The request contains bad syntax or cannot be fulfilled.")
    }
  }

  describe(".apply") {
    it("has `code`") { obj => assert(obj.code == statusCode.intValue)}
    it("has `type`") { obj => assert(obj.`type` == statusCode.reason)}
    it("has `message`") { obj => assert(obj.message == statusCode.defaultMessage)}
  }
}