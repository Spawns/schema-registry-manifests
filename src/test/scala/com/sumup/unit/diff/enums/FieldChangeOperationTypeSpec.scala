package com.sumup.unit.diff.enums

import com.sumup.diff.enums.FieldChangeOperationType
import org.scalatest.{FunSpec, Outcome, fixture}

import scala.Enumeration

class FieldChangeOperationTypeSpec extends FunSpec {
  it("has enums for all `FieldChangeOperationType.String` operations") {
    assert(FieldChangeOperationType.withName(FieldChangeOperationType.String.ADD).toString == "add")
    assert(FieldChangeOperationType.withName(FieldChangeOperationType.String.REMOVE).toString == "remove")
    assert(FieldChangeOperationType.withName(FieldChangeOperationType.String.REPLACE).toString == "replace")
    assert(FieldChangeOperationType.withName(FieldChangeOperationType.String.COPY).toString == "copy")
    assert(FieldChangeOperationType.withName(FieldChangeOperationType.String.TEST).toString == "test")
    assert(FieldChangeOperationType.withName(FieldChangeOperationType.String.RENAME).toString == "rename")
    assert(FieldChangeOperationType.withName(FieldChangeOperationType.String.MOVE).toString == "move")
  }
}
