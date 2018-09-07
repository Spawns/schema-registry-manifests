package com.sumup.dto

import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

object Consumer {
  def apply(name: String, schemaName: String, schemaMajorVersion: Int, schemaMinorVersion: Int): Consumer = {
    Consumer(Some(ShortObjectId()), name, schemaName, schemaMajorVersion, schemaMinorVersion)
  }
}

@ApiModel
case class Consumer(
                     @(ApiModelProperty @field)(value = "MongoDB ObjectId", required = true)
                     _id: Option[ShortObjectId],
                     @(ApiModelProperty @field)(value = "name of the Consumer", example = "transactions-app", required = true)
                     name: String,
                     @(ApiModelProperty @field)(value = "name of the Schema it uses", example = "transactions", required = true)
                     schemaName: String,
                     @(ApiModelProperty @field)(value = "major version of the Schema it uses", example = "1", required = true)
                     schemaMajorVersion: Int,
                     @(ApiModelProperty @field)(value = "minor version of the Schema it uses", example = "2", required = true)
                     schemaMinorVersion: Int
                   )

