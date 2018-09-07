package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType

object LongField {
  def apply(name: String, isIdentity: Boolean = false): LongField = new LongField(name, FieldType.LONG, isIdentity)
}

case class LongField(
                      override val name: String,
                      override val `type`: FieldType,
                      override val isIdentity: Boolean
                    ) extends PrimitiveField(name, `type`, isIdentity)
