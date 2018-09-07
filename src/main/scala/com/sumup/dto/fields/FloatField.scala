package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType

object FloatField {
  def apply(name: String, isIdentity: Boolean = false): FloatField = new FloatField(name, FieldType.FLOAT, isIdentity)
}

case class FloatField(
                       override val name: String,
                       override val `type`: FieldType,
                       override val isIdentity: Boolean
                     ) extends PrimitiveField(name, `type`, isIdentity)
