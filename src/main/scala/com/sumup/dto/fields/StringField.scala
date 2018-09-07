package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType

object StringField {
  def apply(name: String, isIdentity: Boolean = false): StringField = new StringField(name, FieldType.STRING, isIdentity)
}

case class StringField(
                        override val name: String,
                        override val `type`: FieldType,
                        override val isIdentity: Boolean
                      ) extends PrimitiveField(name, `type`, isIdentity)
