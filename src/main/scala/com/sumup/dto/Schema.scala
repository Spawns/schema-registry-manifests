package com.sumup.dto

import com.sumup.dto.fields.{CompositeFieldLike, Field}
import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

object Schema {
  def apply(name: String, applicationId: String, majorVersion: Int, minorVersion: Int, fields: List[Field]): Schema = {
    Schema(Some(ShortObjectId()), name, applicationId, majorVersion, minorVersion, fields)
  }
}

@ApiModel
case class Schema(
                   @(ApiModelProperty @field)(value = "MongoDB ObjectId", required = true)
                   _id: Option[ShortObjectId],
                   @(ApiModelProperty @field)(value = "name of the Schema", example = "transactions", required = true)
                   name: String,
                   @(ApiModelProperty @field)(value = "unique ID of the application producing this schema", example = "transactions-app", required = true)
                   applicationId: String,
                   @(ApiModelProperty @field)(value = "major version of the Schema", example = "2", required = true)
                   majorVersion: Int,
                   @(ApiModelProperty @field)(value = "minor version of the Schema", example = "9", required = true)
                   minorVersion: Int,
                   @(ApiModelProperty @field)(value = "fields of the Schema", required = true)
                   fields: List[Field]
                 ) extends CompositeFieldLike

