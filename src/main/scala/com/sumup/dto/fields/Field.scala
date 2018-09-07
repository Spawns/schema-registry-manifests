package com.sumup.dto.fields

import com.sumup.dto.FieldType
import com.sumup.dto.fields.exceptions.FieldException
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "An *abstract* field entity that's part of a Schema")
abstract class Field(
                      @(ApiModelProperty @field)(value = "name", example = "transaction_id", required = true)
                      val name: String,
                      @(ApiModelProperty @field)(value = "type of the field", dataType = "com.sumup.dto.FieldType$", required = true)
                      val `type`: FieldType.FieldType,
                      @(ApiModelProperty @field)(value = "whether the field will be used as an identity field and for uniqueness", required = false)
                      val isIdentity: Boolean = false
                    ) {
  // NOTE: Array methods
  @(ApiModelProperty @field)(value = "array items type", dataType = "com.sumup.dto.FieldType$", required = false)
  def items: FieldType.FieldType = {
    throw FieldException(s"Not a field of type: `array`")
  }

  def items_=(newFields: List[Field]): Unit = throw FieldException(s"Not a field of type: `array`")

  // NOTE: Record methods
  @(ApiModelProperty @field)(value = "record fields", required = false)
  def fields: List[Field] = throw FieldException(s"Not a field of type: `record`")
  def fields_=(newFields: List[Field]): Unit = throw FieldException(s"Not a field of type: `record`")
}


