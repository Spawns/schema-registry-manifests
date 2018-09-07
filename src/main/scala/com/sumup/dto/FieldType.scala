package com.sumup.dto

object FieldType extends Enumeration {
  final type FieldType = Value

  object String {
    final val NULL = "null"
    final val INT = "int"
    final val BOOL = "boolean"
    final val LONG = "long"
    final val FLOAT = "float"
    final val DOUBLE = "double"
    final val BYTES = "bytes"
    final val STRING = "string"
    final val ARRAY = "array"
    final val RECORD = "record"
    final val ENUM = "enum"

    // NOTE: Manually construct the string to be able to use it in annotations as a constant.
    final val ALL_ALLOWED_TYPES = "null,int,boolean,long,float,double,bytes,string,array,record,enum"
  }

  final val NULL: FieldType.Value = Value(String.NULL)
  final val BOOL: FieldType.Value = Value(String.BOOL)
  final val INT: FieldType.Value = Value(String.INT)
  final val LONG: FieldType.Value = Value(String.LONG)
  final val FLOAT: FieldType.Value = Value(String.FLOAT)
  final val DOUBLE: FieldType.Value = Value(String.DOUBLE)
  final val BYTES: FieldType.Value = Value(String.BYTES)
  final val STRING: FieldType.Value = Value(String.STRING)
  final val ARRAY: FieldType.Value = Value(String.ARRAY)
  final val RECORD: FieldType.Value = Value(String.RECORD)
  final val ENUM: FieldType.Value = Value(String.ENUM)

  final val enumFields = List(FieldType.DOUBLE, FieldType.LONG, FieldType.INT, FieldType.STRING, FieldType.FLOAT)

  def isEnumFieldType(s: String): Boolean = enumFields.exists(_.toString == s)

  def isType(s: String): Boolean = values.exists(_.toString == s)
}
