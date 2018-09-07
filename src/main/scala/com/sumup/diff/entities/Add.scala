package com.sumup.diff.entities

import com.sumup.diff.enums.FieldChangeOperationType.FieldChangeOperationType
import spray.json.JsValue

class Add(
           override val operation: FieldChangeOperationType,
           override val path: String,
           val value: JsValue
         ) extends Operation(operation, path)
