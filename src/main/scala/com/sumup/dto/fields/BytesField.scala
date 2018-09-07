package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType

object BytesField {
  def apply(name: String, isIdentity: Boolean = false): BytesField = new BytesField(name, FieldType.BYTES, isIdentity)
}

case class BytesField(
                       override val name: String,
                       override val `type`: FieldType,
                       override val isIdentity: Boolean
                     ) extends PrimitiveField(name, `type`, isIdentity)
