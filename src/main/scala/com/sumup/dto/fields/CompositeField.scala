package com.sumup.dto.fields

import com.sumup.dto.FieldType.FieldType

abstract class CompositeField(
                               name: String,
                               `type`: FieldType,
                               isIdentity: Boolean = false,
                               override val fields: List[Field]
                             ) extends Field(name, `type`, isIdentity) with CompositeFieldLike {
  this: {
    def copy(name: String, `type`: FieldType, isIdentity: Boolean, fields: List[Field]): Field
  } =>
}
