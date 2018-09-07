package com.sumup.diff.entities

import com.sumup.diff.enums.FieldChangeOperationType.FieldChangeOperationType

class Copy(
            override val operation: FieldChangeOperationType,
            val from: String,
            override val path: String
          ) extends Operation(operation, path)
