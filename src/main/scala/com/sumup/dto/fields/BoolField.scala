package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType

object BoolField {
  def apply(name: String, isIdentity: Boolean = false): BoolField = new BoolField(name, FieldType.BOOL, isIdentity)
}

case class BoolField(
                      override val name: String,
                      override val `type`: FieldType,
                      override val isIdentity: Boolean
                    ) extends PrimitiveField(name, `type`, isIdentity)
