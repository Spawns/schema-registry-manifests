package com.sumup.dto.requests

import com.sumup.dto.fields.Field
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "A Schema request entity used for creation/update of a Schema")
case class SchemaRequest(
                          @(ApiModelProperty @field)(value = "name of the Schema", example = "transactions", required = true)
                          name: String,
                          @(ApiModelProperty @field)(value = "unique ID of the application producing this schema", example = "transactions-app", required = true)
                          applicationId: String,
                          @(ApiModelProperty @field)(value = "major version of the Schema", example = "2", required = true)
                          majorVersion: Int,
                          @(ApiModelProperty @field)(value = "minor version of the Schema", example = "9", required = true)
                          minorVersion: Int,
                          @(ApiModelProperty @field)(value = "list of fields that the schema specifies", required = true)
                          fields: List[Field]
                        )
