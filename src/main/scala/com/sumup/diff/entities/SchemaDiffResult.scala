package com.sumup.diff.entities

import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "A Schema difference result after a schema compatibility check")
case class SchemaDiffResult(
   @(ApiModelProperty @field)(value = "whether the submitted schema is exactly the same as the stored one", example = "false")
   isSameSchema: Boolean = false,
   @(ApiModelProperty @field)(value = "whether the submitted schema is upgradable with a major version bump", example = "true")
   isMajorUpgradable: Boolean = false,
   @(ApiModelProperty @field)(value = "whether the submitted schema is upgradable with a minor version bump", example = "true")
   isMinorUpgradable: Boolean = false,
   @(ApiModelProperty @field)(value = "whether the submitted schema is a major version upgrade over the previous one", example = "false")
   isMajorUpgrade: Boolean = false,
   @(ApiModelProperty @field)(value = "whether the submitted schema is a minor version upgrade over the previous one", example = "false")
   isMinorUpgrade: Boolean = false,
   @(ApiModelProperty @field)(value = "list of field changes between submitted schema and the stored one")
   fieldChanges: List[Operation] = List[Operation]()
)
