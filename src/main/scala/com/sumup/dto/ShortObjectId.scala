package com.sumup.dto

import java.time.Instant
import java.util.Date

import io.swagger.annotations.{ApiModel, ApiModelProperty}
import org.mongodb.scala.bson.ObjectId

import scala.annotation.meta.field

@ApiModel(description = "A shorter representation of a MongoDB ObjectId")
class ShortObjectId(
                     @(ApiModelProperty @field)
                     (
                       name = "$id",
                       value = "`$id` of MongoDB ObjectId",
                       example = "1054232",
                       required = true
                     )
                     val id: Int,
                     @(ApiModelProperty @field)
                     (
                       value = "`timestamp` of MongoDB ObjectId",
                       example = "2017-12-04T12:31:11Z",
                       required = true,
                       dataType = "DateTime"
                     )
                     val timestamp: Instant) {
  def toObjectId: ObjectId = {
    new ObjectId(Date.from(timestamp), id)
  }
}

object ShortObjectId {
  def apply(): ShortObjectId = ShortObjectId.fromObjectId(new ObjectId)
  def fromObjectId(objectId: ObjectId): ShortObjectId = {
    new ShortObjectId(objectId.getCounter, objectId.getDate.toInstant)
  }
}

