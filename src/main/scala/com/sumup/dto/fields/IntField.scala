package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType

object IntField {
  def apply(name: String, isIdentity: Boolean = false): IntField = new IntField(name, FieldType.INT, isIdentity)
}

case class IntField(
                     override val name: String,
                     override val `type`: FieldType,
                     override val isIdentity: Boolean
                   ) extends PrimitiveField(name, `type`, isIdentity)

