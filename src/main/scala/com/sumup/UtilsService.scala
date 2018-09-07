package com.sumup

import com.sumup.dto.fields.{Field, RecordField}

class UtilsService {
  def sortFields(payload: Seq[Field]): Seq[Field] = {
    val fields = payload.map {
      case recordField: RecordField => recordField.copy(fields = sortFields(recordField.fields).toList)
      case f: Field => f
    }

    fields.sortBy(f => (f.name, f.`type`))
  }
}
