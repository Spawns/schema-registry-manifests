package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType

object EnumField {
  def apply[A](name: String, allowedValues: List[A], valueType: FieldType, isIdentity: Boolean = false): EnumField[A] = {
    new EnumField[A](name, FieldType.ENUM, isIdentity, allowedValues, valueType)
  }
}

case class EnumField[A](
                        override val name: String,
                        override val `type`: FieldType,
                        override val isIdentity: Boolean,
                        allowedValues: List[A],
                        valueType: FieldType
                      ) extends Field(name, `type`, isIdentity)
