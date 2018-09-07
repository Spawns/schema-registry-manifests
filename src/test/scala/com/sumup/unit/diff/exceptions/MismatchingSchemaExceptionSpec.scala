package com.sumup.unit.diff.exceptions

import com.sumup.diff.exceptions.MismatchingSchemaException
import org.scalatest.{Outcome, fixture}

class MismatchingSchemaExceptionSpec extends fixture.FunSpec {
  type FixtureParam = MismatchingSchemaException
  val message = "Schemas don't match"

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = MismatchingSchemaException(message)
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
