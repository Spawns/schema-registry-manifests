package com.sumup.diff.enums

object FieldChangeOperationType extends Enumeration {
  type FieldChangeOperationType = Value

  object String {
    final val ADD = "add"
    final val REMOVE = "remove"
    final val REPLACE = "replace"
    final val RENAME = "rename"
    final val MOVE = "move"
    final val COPY = "copy"
    final val TEST = "test"

    // NOTE: Manually construct the string to be able to use it in annotations as a constant.
    final val ALL_ALLOWED_TYPES = "add,remove,replace,rename,move,copy,test"
  }

  val ADD: FieldChangeOperationType.Value = Value(String.ADD)
  val REMOVE: FieldChangeOperationType.Value = Value(String.REMOVE)
  val REPLACE: FieldChangeOperationType.Value = Value(String.REPLACE)
  val RENAME: FieldChangeOperationType.Value = Value(String.RENAME)
  val COPY: FieldChangeOperationType.Value = Value(String.COPY)
  val MOVE: FieldChangeOperationType.Value = Value(String.MOVE)
  val TEST: FieldChangeOperationType.Value = Value(String.TEST)
}
