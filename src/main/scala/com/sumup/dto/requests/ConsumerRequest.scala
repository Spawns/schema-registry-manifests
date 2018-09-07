package com.sumup.dto.requests

import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "A Consumer request entity used for creation of a Consumer")
case class ConsumerRequest(
                          @(ApiModelProperty @field)(value = "name of the consumer", example = "transactions-app", required = true)
                          name: String,
                          @(ApiModelProperty @field)(value = "name of the Schema it uses", example = "transactions", required = true)
                          schemaName: String,
                          @(ApiModelProperty @field)(value = "major version of the Schema it uses", example = "1", required = true)
                          schemaMajorVersion: Int,
                          @(ApiModelProperty @field)(value = "minor version of the Schema it uses", example = "2", required = true)
                          schemaMinorVersion: Int
                        )
