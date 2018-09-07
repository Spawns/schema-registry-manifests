package com.sumup.dto.fields

import com.sumup.dto.FieldType.FieldType

abstract class PrimitiveField(override val name: String, override val `type`: FieldType, override val isIdentity: Boolean)
  extends Field(name, `type`, isIdentity) {
  this: {
    def copy(name: String, `type`: FieldType, isIdentity: Boolean): Field
  } =>
}

