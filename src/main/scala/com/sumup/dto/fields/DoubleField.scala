package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType

object DoubleField {
  def apply(name: String, isIdentity: Boolean = false): DoubleField = {
    new DoubleField(name, FieldType.DOUBLE, isIdentity)
  }
}

case class DoubleField(
                        override val name: String,
                        override val `type`: FieldType,
                        override val isIdentity: Boolean
                      ) extends PrimitiveField(name, `type`, isIdentity)
