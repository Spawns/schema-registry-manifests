package com.sumup.unit.dto.fields.exceptions

import com.sumup.dto.FieldType
import com.sumup.dto.fields.exceptions.FieldException
import org.scalatest.{Outcome, fixture}

class FieldExceptionSpec extends fixture.FunSpec {
  type FixtureParam = FieldException
  val message = "Something went wrong"
  val items = FieldType.INT

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = FieldException(message)
    test(fixture)
  }

  describe("#apply") {
    it("has `message`") { fixture =>
      assert(fixture.message == message)
    }

    it("has default `cause` that is `null`") { fixture =>
      assert(fixture.cause == null)
    }
  }
}
