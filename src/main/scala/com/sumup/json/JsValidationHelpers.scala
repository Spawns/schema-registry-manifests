package com.sumup.json

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsFalse, JsTrue, JsValue, deserializationError, JsonFormat}

object JsValidationHelpers {
  def getBooleanFieldOrThrow(fieldName: String, field: Option[JsValue], defaultValue: Boolean): Boolean = {
    field.orNull match {
      case JsTrue => true
      case JsFalse => false
      case null => defaultValue
      case _ => deserializationError(s"`$fieldName` is not a boolean")
    }
  }

  def getPresentStringFieldOrThrow(fieldName: String, field: Option[JsValue]): String = {
    val stringField = field.fold("")(_.convertTo[String])
    if (stringField.trim.isEmpty) {
      deserializationError(s"`$fieldName` is blank/empty")
    }

    stringField
  }

  def getPositiveIntOrThrow(fieldName: String, field: Option[JsValue]): Int = {
    val intField: JsValue = field.getOrElse(throw deserializationError(s"`$fieldName` is empty"))
    var intValue = 0

    try {
      intValue = intField.convertTo[Int]
    } catch {
      case _: DeserializationException =>
        deserializationError(s"`$fieldName` is not an int")
    }

    if (intValue.isNaN || intValue < 1) {
      deserializationError(s"`$fieldName` is not a valid positive int")
    }

    intValue
  }

  def getPresentListOrThrow[A:JsonFormat](fieldName: String, field: Option[JsValue]): List[A] = {
    if (field == None) {
      deserializationError(s"`$fieldName` is blank/empty")
    }

    val list = field.get.convertTo[List[A]]

    if (list.size == 0) {
      deserializationError(s"`$fieldName` is blank/empty")
    }

    list
  }

  def getPresentAndKnownFieldTypeOrThrow(fieldName: String, field: Option[JsValue]): FieldType = {
    val fieldTypeString = JsValidationHelpers.getPresentStringFieldOrThrow(fieldName, field)

    if (!FieldType.isType(fieldTypeString)) {
      deserializationError(s"`$fieldTypeString` is not a known field type")
    }

    FieldType.withName(fieldTypeString)
  }

  def getPresentAndKnownEnumFieldTypeOrThrow(fieldName: String, field: Option[JsValue]): FieldType = {
    val fieldTypeString = JsValidationHelpers.getPresentStringFieldOrThrow(fieldName, field)

    if (!FieldType.isEnumFieldType(fieldTypeString)) {
      deserializationError(s"`$fieldTypeString` is not a known enum field type")
    }

    FieldType.withName(fieldTypeString)
  }
}
