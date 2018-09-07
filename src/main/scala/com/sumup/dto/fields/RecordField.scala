package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.FieldType.FieldType

object RecordField {
  def apply(name: String, fields: List[Field], isIdentity: Boolean = false): RecordField = {
    new RecordField(name, FieldType.RECORD, isIdentity, fields)
  }
}

case class RecordField(
                        override val name: String,
                        override val `type`: FieldType,
                        override val isIdentity: Boolean,
                        override val fields: List[Field]
                      ) extends CompositeField(name, `type`, isIdentity, fields)
