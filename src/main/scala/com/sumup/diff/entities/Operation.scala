package com.sumup.diff.entities

import com.sumup.diff.enums.FieldChangeOperationType.FieldChangeOperationType

class Operation(val operation: FieldChangeOperationType, val path: String)
