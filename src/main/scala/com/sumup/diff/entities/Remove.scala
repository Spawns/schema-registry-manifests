package com.sumup.diff.entities

import com.sumup.diff.enums.FieldChangeOperationType.FieldChangeOperationType
import spray.json.JsValue

class Remove(
              override val operation: FieldChangeOperationType,
              override val path: String,
              val oldValue: Option[JsValue] = None
            ) extends Operation(operation, path)
