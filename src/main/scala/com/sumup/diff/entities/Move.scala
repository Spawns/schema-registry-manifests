package com.sumup.diff.entities

import com.sumup.diff.enums.FieldChangeOperationType.FieldChangeOperationType

class Move(
            override val operation: FieldChangeOperationType,
            val from: String,
            override val path: String
          ) extends Operation(operation, path)
