package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType

object ArrayField {
  def apply(name: String, items: FieldType, isIdentity: Boolean = false): ArrayField = {
    new ArrayField(name, FieldType.ARRAY, isIdentity, items)
  }
}

case class ArrayField(
                       override val name: String,
                       override val `type`: FieldType,
                       override val isIdentity: Boolean,
                       override val items: FieldType
                     ) extends Field(name, `type`, isIdentity)
